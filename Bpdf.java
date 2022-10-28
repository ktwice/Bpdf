package bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * pdf slicer
 * @author ktwice
 */
public class Bpdf {
    final static float IMAGE_SCALE = 7; // image quality. 1 = 72 DPI  
    final static Pattern pOrderSeparator = Pattern.compile("[^\\d]\\d{40}");
    final static Pattern pLineSeparator = Pattern.compile("\n+");
    final static Pattern pWordSeparator = Pattern.compile("[^\\d\\p{L}]+");
    public static void main(String[] args) {
        slice(new File(args[0]));
    }
    private static void slice(File f) {
        if(f.isDirectory()) {
            try { 
                Files.newDirectoryStream(f.toPath(), "*.pdf")
                        .forEach((path)->pdfSlice(path.toFile()));
            } catch (IOException e) {
                System.out.println("IOException *** " + e.getMessage());
            }
        } else {
            pdfSlice(f);
        }
    }
    private static void pdfSlice(File f) {
        try {
            System.out.println(f.getPath());
            PDDocument pdd = Loader.loadPDF(f);
            File dir = mkdirImages(f);
            int pcount = pdd.getNumberOfPages();
            for (int pno=1; pno<=pcount; pno++) {
                pageImageSlice(pdd, pno, dir);
            }
//            System.out.println("" + pcount + " pages readed.");
        } catch (IOException e) {
            System.out.println("IOException *** " + e.getMessage());
        }
    }
    private static File mkdirImages(File f) {
        String fname = f.getName();
        String dname = fname.substring(0, fname.lastIndexOf('.'));
        File dir = new File(f.getParentFile(), dname);
        dir.mkdir(); // create images-destination-dir
        return dir;
    }
    private static String[] pageTextSlice(final String pageText) {
        String[] orders = pOrderSeparator.split(pageText);
        return Arrays.copyOf(orders, orders.length - 1);
    }
    private static String getPageText(PDDocument pdd, int pageno) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageno);
        stripper.setEndPage(pageno);
        return stripper.getText(pdd);
    }
    private static void pageImageSlice(PDDocument pdd
            , int pno
            , File dir) throws IOException {
        String[] orderTexts = pageTextSlice(getPageText(pdd,pno));
        if(orderTexts.length == 0) return;
        System.out.println(" " + orderTexts.length + " orders on page# " + pno);
        PDFRenderer r = new PDFRenderer(pdd);
        BufferedImage image = r.renderImage(pno - 1, IMAGE_SCALE);
        switch(orderTexts.length) {
            case 1:
                ImageIO.write(image
                    , "JPEG"
                    , new File(dir, name(orderTexts[0]) + "p" + pno + ".jpg")
                );
                break;
            case 2:
                int h = image.getHeight();
                int w = image.getWidth();
                ImageIO.write(image.getSubimage(0, 0, w, h / 2)
                    , "JPEG"
                    , new File(dir, name(orderTexts[0]) + "t" + pno + ".jpg")
                );
                ImageIO.write(image.getSubimage(0, h / 2, w, h - h / 2)
                    , "JPEG"
                    , new File(dir, name(orderTexts[1]) + "b" + pno + ".jpg")
                );
                break;
        }
    }
    private static String name(final String orderText) {
        Optional<String> sline = pLineSeparator.splitAsStream(orderText)
            .filter((line)->line.contains("Плательщик"))
            .findFirst();
        if(!sline.isPresent()) return "";
        Optional<String> sname = pWordSeparator.splitAsStream(sline.get())
            .filter(x->!x.isEmpty())
            .reduce((r,x)->r + "-" + x);
        if(!sname.isPresent()) return "";
        return sname.get() + ".";
    }
}
