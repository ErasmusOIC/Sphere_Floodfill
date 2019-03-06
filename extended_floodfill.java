package New3dFloodfill;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import myClasses.Point3D;
import myClasses.getPointRois;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayDeque;
import java.util.Deque;

public class extended_floodfill implements PlugIn {


    public void run(String s) {
        if(s.equals("sphere")){

            String[] filltype = {"above threshold","below threshold"};
            String[] stattype = {"max","min","mean","sd"};
            String[] prectype = {"more than", "less than"};

            double threshold=0;
            int fill=0;
            int stat=0;
            int radius=5;
            boolean prec = false;
            int percentage=10;
            int method=0;

            GenericDialog gd = new GenericDialog("Floodfill with spheres");

            gd.addNumericField("Threshold",0,3);
            gd.addChoice("Filltype",filltype,filltype[0]);
            gd.addChoice("Measure",stattype,stattype[0]);
            gd.addNumericField("Sphere radius (px)",radius,0);
            gd.addCheckbox("Percentage",prec);
            gd.addNumericField("Percentage",percentage,0);
            gd.addChoice("Method",prectype,prectype[0]);
            gd.showDialog();

            if(gd.wasCanceled()){
                return;
            }


            threshold = gd.getNextNumber();
            fill = gd.getNextChoiceIndex();
            stat = gd.getNextChoiceIndex();
            radius = (int) gd.getNextNumber();
            prec = gd.getNextBoolean();
            percentage = (int) gd.getNextNumber();
            method = gd.getNextChoiceIndex();

            ImagePlus imp = WindowManager.getCurrentImage();


            floodfill(imp,threshold,fill,stat,radius,prec,percentage,method);


        }

        if(s.equals("pixel")){
            String[] filltype = {"above threshold","below threshold","between threshold"};



            double threshold=0;
            int fill=0;


            GenericDialog gd = new GenericDialog("Floodfill with spheres");

            gd.addNumericField("Threshold",0,3);
            gd.addChoice("Filltype",filltype,filltype[0]);
            gd.showDialog();

            if(gd.wasCanceled()){
                return;
            }


            threshold = gd.getNextNumber();
            fill = gd.getNextChoiceIndex();


            ImagePlus imp = WindowManager.getCurrentImage();


            floodfill(imp,threshold,fill);
        }
    }

    public void floodfill(ImagePlus imp,double thr, int type){

    }


    public<T extends RealType<T> & NativeType<T>> void floodfill(ImagePlus imp, double thr, int type, int stattype, int radius, boolean prec, int percentage, int method){

        int xs,ys,zs,xdim,ydim,zdim,x,y,z;
        int vol_cnt=0;
        boolean thr_test;


        Img<T> img = ImagePlusAdapter.wrap(imp);
        ImageStack ims = imp.getImageStack();
        Point3D[] pta;

        try {
            pta = new getPointRois(imp).getRois();
        }catch(IndexOutOfBoundsException e){
            IJ.showMessage("no point selection present");
            return;
        }

        Calibration c = imp.getCalibration();

        xdim = ims.getWidth();
        ydim = ims.getHeight();
        zdim = ims.getSize();
        zs = imp.getSlice();

        boolean[] pta_test = new boolean[pta.length];

        for(int i=0;i<pta.length;i++) {

            if (!pta_test[i]) {

                xs = (int) pta[i].getX();
                ys = (int) pta[i].getY();
                if (pta[i].getZ() != 0) {
                    zs = (int) pta[i].getZ();
                }

                //IJ.showMessage(" "+xs+" "+ys+" "+zs);

                int[] xyz = new int[]{xs, ys, zs};

                Deque<int[]> stack = new ArrayDeque<>(xdim * ydim * zdim);

                boolean[][][] fill = new boolean[xdim][ydim][zdim + 1];
                boolean[][][] stackd = new boolean[xdim + 2][ydim + 2][zdim + 3];


                stack.push(xyz);
                stackd[xs + 1][ys + 1][zs + 1] = true;

                vol_cnt = 0;

                while (stack.size() > 0) {

                    xyz = stack.pop();
                    HyperSphere<T> h = getSphere(xyz, img, radius);

                    if (testbounds(xyz, ims)) {
                        if(!prec) {
                            thr_test = testThreshold(h, thr, type, stattype);
                        }else{
                            thr_test = testThreshold(h, thr, percentage, type, method);
                        }
                    } else {
                        thr_test = false;

                    }

                    if (thr_test && !fill[xyz[0]][xyz[1]][xyz[2]]) {


                        fill[xyz[0]][xyz[1]][xyz[2]] = true;

                        vol_cnt++;
                        x = xyz[0];
                        y = xyz[1];
                        z = xyz[2];

                        for (int j = 0; j < 27; j++) {
                            int ud = (j % 3) - 1;
                            int vd = ((j / 3) % 3) - 1;
                            int wd = (j / 9) - 1;


                            if (!(ud == 0 && vd == 0 && wd == 0)) {

                                if (!stackd[x + ud + 1][y + vd + 1][z + wd + 1]) {
                                    int[] xyz_t = new int[]{x + ud, y + vd, z + wd};
                                    stackd[x + ud + 1][y + vd + 1][z + wd + 1] = true;
                                    stack.push(xyz_t);
                                }

                            }

                        }
                    }
                    IJ.showStatus("Point " + (i + 1) + " / " + pta.length + " stacksize: " + stack.size() + " Filled voxels:  " + vol_cnt);

                }



                ImageStack ims_result = new ImageStack(xdim, ydim);

                for (int a = 1; a <= zdim; a++) {
                    ImageProcessor ip_result = new ByteProcessor(xdim, ydim);
                    for (int j = 0; j < xdim; j++) {
                        for (int k = 0; k < ydim; k++) {

                            if (fill[j][k][a]) {
                                ip_result.putPixel(j, k, 255);
                            }

                        }
                    }
                    ims_result.addSlice(ip_result);
                    IJ.showProgress(a, zdim);


                }

                ImagePlus imp2 = new ImagePlus("Results_" + (i + 1), ims_result);
                imp2.setCalibration(c);
                imp2.setDimensions(1, zdim, 1);
                imp2.show();

                for(int j=i+1;j<pta.length;j++){
                    xs = (int) pta[j].getX();
                    ys = (int) pta[j].getY();

                    if (pta[i].getZ() != 0) {
                        zs = (int) pta[i].getZ();
                    }

                    if(fill[xs][ys][zs]){
                        pta_test[j] = true;
                    }

                }

            }
        }














    }

    public boolean testThreshold(int v, double thr, int type){

        boolean result;

        switch(type){
            case 0: result = v > thr;
            break;
            case 1: result = v < thr;
            break;
            default : result = false;
        }

        return result;

    }

    public boolean testThreshold(int v, double thr[]){
        boolean result = v > thr[0] && v < thr[1];

        return result;
    }

    public<T extends RealType<T> & NativeType<T>> boolean testThreshold(HyperSphere<T> h, double thr, int type, int stattype){
        boolean result;

        double[] stats = sphereStats(h);

        switch(type){
            case 0: result = stats[stattype] > thr;
            break;
            case 1: result = stats[stattype] < thr;
            break;
            default : result = false;
        }

        return result;
    }

    public<T extends RealType<T> & NativeType<T>> boolean testThreshold(HyperSphere<T> h, double thr, int prec, int type, int type2){
        boolean result;
        int num = 0;
        int thr_num = 0;

        for(T v:h){

            double value;

            try{
                value = v.getRealDouble();
            }catch(ArrayIndexOutOfBoundsException e){
                value = 0;
            }

            if(testThreshold((int) value,thr,type)){
                thr_num++;
            }
            num++;

        }

        result = testThreshold(  (int)( ((double) thr_num/ (double)num)*100 ), prec, type2 );

        return result;
    }




    public int getVoxel(int[] xyz, ImageStack ims){

        int v=0;

        if(testbounds(xyz,ims)){
            ImageProcessor ip = ims.getProcessor(xyz[2]);
            v = ip.get(xyz[0], xyz[1]);
        }

        return v;
    }


    public boolean testbounds(int[] xyz_, ImageStack ims_){
        boolean bounds = true;

        if(xyz_[2]<=ims_.size() && xyz_[2] > 0 ){

            ImageProcessor ip = ims_.getProcessor(xyz_[2]);

            if(!(xyz_[0] < ip.getWidth() && xyz_[0] >= 0 && xyz_[1] < ip.getHeight() && xyz_[1] >= 0)){

                bounds = false;
            }
        }else{
            bounds = false;
        }

        return bounds;
    }

    public<T extends RealType<T> & NativeType<T>> HyperSphere<T> getSphere(int[] xyz, Img<T> img, int radius){
        Point p = new Point(xyz.length);

        for(int i=0;i<xyz.length;i++) {
            p.setPosition(xyz[i],i);
        }

        HyperSphere<T> h = new HyperSphere<>(img,p,radius);

        return h;
    }

    public<T extends RealType<T> & NativeType<T>> double[] sphereStats(HyperSphere<T> h){
        double[] out = new double[5];
        double max = 0;
        double min = 100000;
        double tot = 0;
        double sd = 0;
        double mean = 0;
        int num = 0;
        double value = 0;

        for(T v:h){

            try{
                value = v.getRealDouble();
            }catch(ArrayIndexOutOfBoundsException e){
                value = 0;
            }

            if(value > max){
                max = value;
            }

            if(value < max){
                min = value;
            }

            tot = tot + value;

            num++;

        }

        mean = tot/num;

        for(T v:h){

            try{
                value = v.getRealDouble();
            }catch(ArrayIndexOutOfBoundsException e){
                value = 0;
            }

            sd = sd + Math.pow(value-mean,2);

        }


        sd = Math.sqrt(sd)/(double)num;

        out[0] = max;
        out[1] = min;
        out[2] = mean;
        out[3] = sd;
        out[4] = num;

        return out;


    }




}
