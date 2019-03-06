package myClasses;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;


import java.awt.*;
import java.util.MissingFormatArgumentException;

public class getPointRois {

    Point3D[] rois;

    public getPointRois(ImagePlus imp){
        try{
            rois = getRoi(imp);}
        catch(IndexOutOfBoundsException e){

            throw new IndexOutOfBoundsException();

        }
    }

    private Point3D[] getRoi(ImagePlus imp){

        try{
            String s = imp.getRoi().getClass().getName();
        }catch(NullPointerException e){
            throw new IndexOutOfBoundsException();
        }

        if(imp.getRoi().getClass().getName() != "ij.gui.PointRoi"){
           throw new IndexOutOfBoundsException();
        }



        PointRoi pt = (PointRoi) imp.getRoi();
        Point[] pt_list = pt.getContainedPoints();
        Point3D[] pt_3d = new Point3D[pt_list.length];

        for(int i=0;i<pt_list.length;i++){
            pt_3d[i] = new Point3D(pt_list[i], pt.getPointPosition(i));
        }


        return pt_3d;


    }

    public Point3D[] getRois(){
        return rois;
    }

}
