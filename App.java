package ru.ktwice.bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * pdf slicer
 * @author ktwice
 */
public class App 
{
    final static float IMAGE_SCALE = 7; // image quality. 1 = 72 DPI  
    final static Pattern pOrderSeparator = Pattern.compile("[^\\d]\\d{40}");
    final static Pattern pLineSeparator = Pattern.compile("\n+");
    final static Pattern pWordSeparator = Pattern.compile("[^\\d\\p{L}]+");
    public static void main(String[] args) {
//        if(args.length > 1) IMAGE_SCALE = Integer.parseInt(args[1]);
        slice(new File(args[0]));
    }
    private static void slice(File f) {
        if(!f.isDirectory()) {
            pdfSlice(f);
            return;
        }
        try { 
            System.out.println(f.getPath());
            Files.newDirectoryStream(f.toPath(), "*.pdf")
                .forEach((path)->pdfSlice(path.toFile()));
            System.out.println("Processing of all pdf-files is completed.");
        } catch (IOException e) {
            System.out.println("IOException *** " + e.getMessage());
        }
    }
    private static void pdfSlice(File f) {
        try {
            System.out.println(f.getPath());
            PDDocument pdd = Loader.loadPDF(f);
String s = f.getName();
File dir = new File(f.getParentFile(), s.substring(0, s.lastIndexOf('.')));
dir.mkdir(); // create images-destination-dir
            int pcount = pdd.getNumberOfPages();
            for (int pno=1; pno<=pcount; pno++) 
                pageImageSlice(pdd, pno, dir);
            System.out.println("Processing of " + pcount + " pages is completed.");
        } catch (IOException e) {
            System.out.println("IOException *** " + e.getMessage());
        }
    }
    private static String pageText(PDDocument pdd, int pageno)
            throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageno);
        stripper.setEndPage(pageno);
        return stripper.getText(pdd);
    }
    private static boolean pageImageSlice(PDDocument pdd
            , int pno, File dir) throws IOException {
        String[] orders = pOrderSeparator.split(pageText(pdd,pno));
        if(orders.length < 2)
            return true;
        System.out.println(" " + (orders.length - 1) + " orders on page# " + pno);
        PDFRenderer r = new PDFRenderer(pdd);
        BufferedImage image = r.renderImage(pno - 1, IMAGE_SCALE);
// ImageIO.write(image,"JPEG",new File(dir,"p"+pno+".jpg"));
        int w = image.getWidth();
        int[] h2 = vmargins(image);
// System.out.println("h2="+h2[0]+" "+h2[1]);
        if(orders.length == 2)
            return ImageIO.write(image.getSubimage(0, h2[0], w, h2[1]), "JPEG"
                , new File(dir, name(orders[0]) + "p" + pno + ".jpg")
            );
        int h1 = h2[1] / 2; 
// System.out.println("h1="+h1);
        return ImageIO.write(image.getSubimage(0, h2[0], w, h1), "JPEG"
            , new File(dir, name(orders[0]) + "t" + pno + ".jpg")
        ) && ImageIO.write(image.getSubimage(0, h2[0]+h1, w, h2[1]-h1), "JPEG"
            , new File(dir, name(orders[1]) + "b" + pno + ".jpg")
        );
    }
    private static String name(final String orderText) {
        final String token = "Плательщик";
        Optional<String> sline = pLineSeparator.splitAsStream(orderText)
            .filter((line)->line.contains(token))
            .findFirst();
        if(!sline.isPresent()) return "";
        Optional<String> sname = pWordSeparator.splitAsStream(sline.get())
            .filter(word->!word.isEmpty() && !word.contains(token))
            .reduce((result,word)->result + "-" + word);
        return !sname.isPresent() ? "" : sname.get() + ".";
    }
  static public int[] vmargins(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
//    System.out.println("w="+w);
//    System.out.println("h="+h);
    int[] hlines = img.getRGB(0,0,w,h,null,0,w);
    Set<Integer> pset = new HashSet<>();
    int topmargin = 0;
    for(int y=0; y<h; y++) {
      pset.clear();
      Arrays.stream(hlines, y * w, (y+1) * w).forEach(x->pset.add(x));
      if(pset.size() > 1) break;
      ++topmargin;
    }
    int bottommargin = 0;
    for(int y=h-1; y>topmargin; y--) {
      pset.clear();
      Arrays.stream(hlines, y * w, (y+1) * w).forEach(x->pset.add(x));
      if(pset.size() > 1) break;
      ++bottommargin;
    }
    return new int[] {topmargin, h - bottommargin - topmargin};
  }
}
