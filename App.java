package ru.ktwice.bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.util.function.Predicate;
import java.util.stream.IntStream;
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
    final static Pattern pOrderSeparator = Pattern.compile("\\d{40,}");
//    final static Pattern pLineSeparator = Pattern.compile("\n+");
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
        File dir = null;
        PDFRenderer r = null;
        try {
            System.out.println(f.getPath());
            PDDocument pdd = Loader.loadPDF(f);
            int pcount = pdd.getNumberOfPages();
            for (int pno=1; pno<=pcount; pno++) { 
                String ptext = pageText(pdd,pno);
                long ocount = pOrderSeparator.matcher(ptext).results().count();
                if(ocount == 0) continue;
//                String name1 = name1(ptext);
                System.out.println(" page# " + pno);
//                for(int i=0; i<names.length; i++) {
//System.out.println("<order>"+orders[i]+"</order>");
//                    names[i] = name(orders[i]) + String.valueOf(pno);
//                }
                if(r == null) {
                    String s = f.getName();
                    dir = new File(f.getParentFile(), s.substring(0, s.lastIndexOf('.')));
                    dir.mkdir(); // create images-destination-dir
                    r = new PDFRenderer(pdd);
                }
                BufferedImage image = r.renderImage(pno - 1, IMAGE_SCALE);
        int[] v = vcrop(image);
        String ft = pWordSeparator.matcher(tname(ptext)).replaceAll("-");
        if(ocount == 1) {
            ImageIO.write(image.getSubimage(v[0], v[1], v[2], v[3])
                , "JPEG"
                , new File(dir, ft + pno + "p.jpg")
            );
            continue;
        }
        int h = v[3] / 2; 
        ImageIO.write(image.getSubimage(v[0], v[1], v[2], h)
            , "JPEG"
            , new File(dir, ft + pno + "t.jpg")
        );
        String fb = pWordSeparator.matcher(bname(ptext)).replaceAll("-");
        ImageIO.write(image.getSubimage(v[0], v[1] + h, v[2], v[3] - h)
            , "JPEG"
            , new File(dir, fb + pno + "b.jpg")
        );
            }
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
        String t = stripper.getText(pdd);
// System.out.println("<page page="+pageno+">"+t+"</page>");
        return t;
    }
    private static boolean pageImageSlice(File dir
            , String[] names, BufferedImage image) throws IOException {
// ImageIO.write(image,"JPEG",new File(dir,"p"+pno+".jpg"));
        int[] v = vcrop(image);
// System.out.println("h2="+h2[0]+" "+h2[1]);
        if(names.length == 1)
            return ImageIO.write(image.getSubimage(v[0], v[1], v[2], v[3])
                , "JPEG"
                , new File(dir, names[0] + "p.jpg")
            );
        int h = v[3] / 2; 
// System.out.println("h1="+h1);
        return ImageIO.write(image.getSubimage(v[0], v[1], v[2], h)
            , "JPEG"
            , new File(dir, names[0] + "t.jpg")
        ) && ImageIO.write(image.getSubimage(v[0], v[1] + h, v[2], v[3] - h)
            , "JPEG"
            , new File(dir, names[1] + "b.jpg")
        );
    }
    private static boolean pageImageSlice0(File dir
            , String[] names, BufferedImage image) throws IOException {
// ImageIO.write(image,"JPEG",new File(dir,"p"+pno+".jpg"));
        int w = image.getWidth();
        int[] h2 = vmargins(image);
// System.out.println("h2="+h2[0]+" "+h2[1]);
        if(names.length == 1)
            return ImageIO.write(image.getSubimage(0, h2[0], w, h2[1])
                , "JPEG"
                , new File(dir, names[0] + "p.jpg")
            );
        int h1 = h2[1] / 2; 
// System.out.println("h1="+h1);
        return ImageIO.write(image.getSubimage(0, h2[0], w, h1)
            , "JPEG"
            , new File(dir, names[0] + "t.jpg")
        ) && ImageIO.write(image.getSubimage(0, h2[0]+h1, w, h2[1]-h1)
            , "JPEG"
            , new File(dir, names[1] + "b.jpg")
        );
    }
//    private static String name(final String orderText) {
//        final Charset WIN1251 = Charset.forName("windows-1251");
//        final Charset WIN1252 = Charset.forName("windows-1252");
//        final String token = "Плательщик";
//        Optional<String> sline = pLineSeparator.splitAsStream(orderText)
//            .filter((line)->line.contains(token))
//            .findFirst();
//        if(!sline.isPresent()) {
//        String s1251 = new String(orderText.getBytes(WIN1252),WIN1251);
//System.out.println("<s1251>"+s1251+"</s1251>");
//        sline = pLineSeparator.splitAsStream(s1251)
//            .filter((line)->line.contains(token))
//            .findFirst();
//        if(!sline.isPresent()) return "";
//        }
//        Optional<String> sname = pWordSeparator.splitAsStream(sline.get())
//            .filter(word->!word.isEmpty() && !word.contains(token))
//            .reduce((result,word)->result + "-" + word);
//        return !sname.isPresent() ? "" : sname.get() + ".";
//    }
    static final String NAMETOKEN = "Плательщик";
    private static String tname(final String orderText) {
        int i = orderText.indexOf(NAMETOKEN);
        if(i < 0) return "";
        i += NAMETOKEN.length();
        int i2 = orderText.indexOf("\n", i);
        if(i2 < 0) return orderText.substring(i);
        return orderText.substring(i,i2);
    }
    private static String bname(final String orderText) {
        int i = orderText.lastIndexOf(NAMETOKEN);
        if(i < 0) return "";
        i += NAMETOKEN.length();
        int i2 = orderText.indexOf("\n", i);
        if(i2 < 0) return orderText.substring(i);
        return orderText.substring(i,i2);
    }
  static public int[] vmargins(BufferedImage img) {
    final Set<Integer> pset = new HashSet<>();
    final Predicate<IntStream> yline = (x)->{
        x.forEach(pset::add);
        int size = pset.size();
        pset.clear();
        return (size > 1);
      };
    final int w = img.getWidth();
    final int h = img.getHeight();
    final int[] hlines = img.getRGB(0,0,w,h,null,0,w);
    int top = 0;
    for(int y=0; y<h; y++) {
      if(yline.test(Arrays.stream(hlines, y * w, (y+1) * w))) break;
      ++top;
    }
    int bottom = 0;
    for(int y=h-1; y>top; y--) {
      if(yline.test(Arrays.stream(hlines, y * w, (y+1) * w))) break;
      ++bottom;
    }
    return new int[] {top, h - bottom - top};
  }
  static public int[] vcrop(BufferedImage img) {
    final Set<Integer> pset = new HashSet<>();
    final Predicate<IntStream> yline = (x)->{
        x.forEach(pset::add);
        int size = pset.size();
        pset.clear();
        return (size > 1);
      };
    final int w = img.getWidth();
    final int h = img.getHeight();
    final int[] hlines = img.getRGB(0,0,w,h,null,0,w);
    int top = 0;
    for(int y=0; y<h; y++) {
      if(yline.test(Arrays.stream(hlines, y * w, (y+1) * w))) break;
      ++top;
    }
    int bottom = 0;
    for(int y=h-1; y>top; y--) {
      if(yline.test(Arrays.stream(hlines, y * w, (y+1) * w))) break;
      ++bottom;
    }
    return new int[] {0, top, w, h - bottom - top};
  }
}
