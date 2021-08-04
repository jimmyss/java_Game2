package server;
/**
 * @author Du shimao
 *
 * 该类用于存储游戏进程中玩家的名字，方便netManager访问组内所有玩家，确保正在游戏中的玩家不会再次收到其他玩家的游玩申请
 * 目前仅支持两玩家同台竞技
 */

public class Players {
    private int playerNumber;
    private String name1;
    private String name2;
    private String name3;
    private String name4;

    public Players(int playerNumber,String name1,String name2) {
        this.playerNumber=playerNumber;
        this.name1=name1;
        this.name2=name2;
    }
    public Players(int playerNumber,String name1,String name2,String name3,String name4){
        this.playerNumber=playerNumber;
        this.name1=name1;
        this.name2=name2;
        this.name3=name3;
        this.name4=name4;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
