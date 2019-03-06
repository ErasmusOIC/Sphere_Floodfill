package myClasses;

import java.awt.*;

public class Point3D extends Point {
    int z;

    public Point3D(int x_, int y_, int z_){
        super.setLocation(x_,y_);
        z = z_;
    }

    public Point3D(Point p, int z_){
        super.setLocation(p);
        z = z_;
    }

    public double getZ(){
        return (double) z;
    }


}
