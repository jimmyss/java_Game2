package server;

import server.model.ServerGameMap;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

/**
 * @author Du shimao
 *
 * 该类主要接收用户发出的操作，然后根据操作的标签进行处理，最后生成相应的指令
 * 该类实现了统一生成补给功能，保证了补给同时在对应位置生成在游戏界面上
 * 该类可以访问Server类里面的hashMap和玩家名
 * 该类可以访问数据库，查询玩家信息：用户名、密码、胜负场数
 * serverGameMap为netManager的地图管理器，用于获取指定地图
 * playerGroup用于保存对战中玩家的名字，当游戏开始时会将两名玩家的名字添加到playerGroup的末尾
 * hashMap访问的是server里的静态threadHashMap
 */

public class NetManager implements Runnable{
    private ServerGameMap serverGameMap;
    private int supplyX;
    private int supplyY;
    private int bulletCounter;
    private boolean supplyState;
    private int[][] map;
    private Socket socket;
    private Vector<String> players;
    private Vector<Players> playerGroup;//用于保存对战中的游戏玩家名
    BufferedReader reader;
    BufferedWriter writer;
    int number;
    int groupNumber;
    int gameMode=0;
    boolean available=true;
    boolean groupDismiss=true;
    Vector<String> playerG=new Vector<>();
    private HashMap<String ,NetManager> hashMap;
    private DBUtil dbUtil;

    public NetManager(Socket socket,int number,HashMap<String ,NetManager> threadhashMap,Vector<String> players,Vector<Players> playerGroup,DBUtil dbUtil) {
        this.playerGroup=playerGroup;
        this.players=players;
        hashMap=threadhashMap;
        this.number=number;
        this.socket = socket;
        this.dbUtil = dbUtil;
    }

    /**
     * 运行线程
     * 首先建立输入输出流，然后进入while循环接收信息，接着处理信息并处理异常
     */
        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while(true){
                    String message = reader.readLine();
                    try {
                        dealMessage(message);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            } catch (SocketException exception){
                System.out.println("a client log out");
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

    /**
     *
     * @param name1
     * @param name2
     *
     * 设置本地游戏中玩家名
     */
        public void setPlayerG(String name1,String name2){
            playerG.add(name1);
            playerG.add(name2);
        }

    /**
     *
     * @param state
     * 设置补给状态
     * 若supplyState 为false则说明场上没有补给，反之则有补给
     */
    public void setSupplyState(boolean state){
            supplyState=state;
        }

    /**
     * 用于在地图上的合适位置随机生成补给
     */
    public void createSupply(){//若子弹总数小于6，生成补给
            if (bulletCounter<7&&!supplyState){
                Random random = new Random();
                supplyX = random.nextInt(500);
                supplyY = random.nextInt(500);
                while(true){
                    if(map[supplyY/25][supplyX/25]==1||map[(supplyY+10)/25][supplyX/25]==1||map[supplyY/25][(supplyX+10)/25]==1||map[(supplyY+10)/25][(supplyX+10)/25]==1){
                        supplyX = random.nextInt(500);
                        supplyY = random.nextInt(500);
                    } else break;
                }
                setSupplyState(true);
                try {
                    hashMap.get(playerG.get(0)).writer.write("supply|"+supplyX+"|"+supplyY+"\n");
                    hashMap.get(playerG.get(0)).writer.flush();
                    hashMap.get(playerG.get(1)).writer.write("supply|"+(490-supplyX)+"|"+(490-supplyY)+"\n");
                    hashMap.get(playerG.get(1)).writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    /**
     * 用于同步地图
     */
        public void synchronizeMap(){
            serverGameMap=new ServerGameMap();
            map=serverGameMap.getMap();
        }

    /**
     *
     * @param message
     * @throws IOException
     * @throws SQLException
     *
     * 用于处理接收到的信息，信息为String，格式为：tag|***|***|。其中tag为标识字，用于决定信息的处理方式
     */
        public void dealMessage(String message) throws IOException, SQLException {//服务器接收到NetThread的信息并处理
            String[] strings=message.split("\\|");
            switch (strings[0]){
                case "move"://收到信息为：move|name|x|y|dir
                    hashMap.get(playerG.get(0)).writer.write(message+"\n");
                    hashMap.get(playerG.get(0)).writer.flush();
                    hashMap.get(playerG.get(1)).writer.write(message+"\n");
                    hashMap.get(playerG.get(1)).writer.flush();
                    break;
                case "bullet"://bullet|name|x|y|dir
                    hashMap.get(playerG.get(0)).writer.write(message+"\n");
                    hashMap.get(playerG.get(0)).writer.flush();
                    hashMap.get(playerG.get(1)).writer.write(message+"\n");
                    hashMap.get(playerG.get(1)).writer.flush();
                    bulletCounter--;
                    createSupply();
                    break;
                case "remove"://remove|bulletNumber
                    bulletCounter=Integer.valueOf(strings[1]);
                    setSupplyState(false);
                    break;
                case "end"://end|winnerName
                    //更新胜负数据
                    if(playerG.get(0).equals(strings[1])){
                        //'0'is winner
                        dbUtil.updateWinnerData(playerG.get(0),playerG.get(1));
                    } else {
                        //'1'is winner
                        dbUtil.updateWinnerData(playerG.get(1),playerG.get(0));
                    }
                    hashMap.get(playerG.get(0)).writer.write("end|"+strings[1]+"\n");
                    hashMap.get(playerG.get(0)).writer.flush();
                    hashMap.get(playerG.get(1)).writer.write("end|"+strings[1]+"\n");
                    hashMap.get(playerG.get(1)).writer.flush();
                    break;
                case "success":
                    hashMap.get(strings[1]).writer.write("success|"+dbUtil.getWinData(strings[1])+"\n");
                    hashMap.get(strings[1]).writer.flush();
                    break;
                case "ask"://ask|asker|receiver
                    //提取发起者和接受者名字,并发送给接受者,strings[1]是发起者，2是接受者
                    if (hashMap.get(strings[2]).available&&hashMap.get(strings[2]).groupDismiss){
                        hashMap.get(strings[2]).writer.write("ask|"+strings[1]+"\n");
                        hashMap.get(strings[2]).writer.flush();
                    }else {//这里表示对方正在游戏或者在小组中
                        hashMap.get(strings[1]).writer.write("reject|ask\n");
                    }
                    break;
                case "play"://play|asker|receiver
                    //strings中：第一个是发起者，第二个是接受者
                    //接受者同意游戏，将信息传回发起者
                    playerGroup.add(new Players(2,strings[1],strings[2]));//向server添加游戏组，2代表当前人数
                    groupNumber=playerGroup.size();
                    hashMap.get(strings[1]).groupNumber=groupNumber;
                    playerG.add(strings[1]);
                    playerG.add(strings[2]);//本地游戏组加入两个玩家名
                    groupDismiss=false;
                    available=false;
                    hashMap.get(strings[1]).available=false;
                    hashMap.get(strings[1]).groupDismiss=false;
                    hashMap.get(strings[1]).setPlayerG(strings[1],strings[2]);
                    //这里返回形式是：accept|
                    hashMap.get(strings[1]).writer.write("accept|"+"\n");
                    hashMap.get(strings[1]).writer.flush();
                    break;
                case "reject"://reject|asker
                    //接受者拒绝游戏，将信息传回发起者
                    hashMap.get(strings[1]).writer.write("reject|reject"+"\n");
                    hashMap.get(strings[1]).writer.flush();
                    break;
                case "backLogin"://backLogin|name
                    //将playerGroup中人数减少
                    int playerNum=playerGroup.get(groupNumber-1).getPlayerNumber();
                    available=true;
                    groupDismiss=true;
                    if (playerNum>0){
                        playerGroup.get(groupNumber-1).setPlayerNumber(playerNum-1);
                    }else {
                        playerGroup.remove(groupNumber-1);
                    }
                    break;
                case "quit"://quit|name
                    //移除player名字，并更新nameBox
                    players.remove(strings[1]);
                    //hashMap.remove(strings[1]);
                    String nameMsg="name";
                    for (int i=0;i<players.size();i++){
                        //回传给客户端的nameBox
                        nameMsg+="|";
                        nameMsg+=players.get(i);
                    }
                    for (int i = 0; i < players.size(); i++) {//让用户更新用户名
                        hashMap.get(players.get(i)).writer.write(nameMsg + "\n");
                        hashMap.get(players.get(i)).writer.flush();
                    }
                    //结束对应netManager
                    System.out.println(strings[1]+"  quit");
                    break;
                case "login"://login|name
                    ResultSet rs = dbUtil.checkLogin(strings[1],strings[2]);
                    //从登录用户给出的账号密码来检测查询在数据库表中是否存在相同的账号密码
                    if (rs.next()) {
                        //用户和密码正确
                        if (players.contains(strings[1])) {
                            //如果重名了，返回提醒
                            hashMap.get(strings[1]).writer.write("invalid|" + "\n");
                            hashMap.get(strings[1]).writer.flush();
                        } else {
                            //成功登录，将名字和netManager映射
                            available=true;
                            groupDismiss=true;
                            hashMap.put(strings[1], this);
                            players.add(strings[1]);
                            String nameMessage = "name";
                            for (int i = 0; i < players.size(); i++) {
                                //回传给客户端的nameBox
                                nameMessage += "|";
                                nameMessage += players.get(i);
                            }
                            for (int i = 0; i < players.size(); i++) {//让用户更新用户名
                                hashMap.get(players.get(i)).writer.write(nameMessage + "\n");
                                hashMap.get(players.get(i)).writer.flush();
                            }
                            writer.write("login|"+strings[1]+"\n");
                            writer.flush();
                        }
                    } else {
                        //重新登录
                        writer.write("relog|\n");
                        writer.flush();
                    }
                    break;
                case "signup":
                    //向数据库中添加用户信息
                    writer.write(dbUtil.signUp(strings[1],strings[2])+"\n");
                    writer.flush();
                    break;
                case "start"://收到信息为start|
                    serverGameMap=new ServerGameMap();
                    map=serverGameMap.getMap();
                    hashMap.get(playerG.get(1)).synchronizeMap();
                    hashMap.get(playerG.get(0)).writer.write("create|"+"\n");
                    hashMap.get(playerG.get(0)).writer.flush();
                    hashMap.get(playerG.get(1)).writer.write("create|"+"\n");
                    hashMap.get(playerG.get(1)).writer.flush();
                    bulletCounter=10;
                    break;
            }
        }
    }