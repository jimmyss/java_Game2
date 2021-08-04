package client.model;

/**
 * @author Du shimao
 * Point 类为节点类，保存某一节点在地图中的坐标，以及前置节点
 * prePoint 为前置节点，用于求最短路径
 */

public class Point {
    private boolean flag=true;
    private int num;
    private int x;
    private int y;
    private int[] adjPoint={0,0,0,0};//临近点
    private Point prePoint;

    public Point(int num,int x,int y,int p1,int p2) {
        this.num=num;
        this.x=x;
        this.y=y;
        adjPoint[0]=p1;
        adjPoint[1]=p2;
    }
    public Point(int num,int x,int y,int p1,int p2,int p3) {
        this.num=num;
        this.x=x;
        this.y=y;
        adjPoint[0]=p1;
        adjPoint[1]=p2;
        adjPoint[2]=p3;
    }
    public Point(int num,int x,int y,int p1,int p2,int p3,int p4) {
        this.num=num;
        this.x=x;
        this.y=y;
        adjPoint[0]=p1;
        adjPoint[1]=p2;
        adjPoint[2]=p3;
        adjPoint[3]=p4;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getNum() {
        return num;
    }

    public boolean isFlag(){
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public void setPrePoint(Point prePoint) {
        this.prePoint = prePoint;
    }

    public Point getPrePoint() {
        return prePoint;
    }
}
