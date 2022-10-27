//package bpdf;

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
        if(!f.isDirectory())
            pdfSlice(f);
        else try { 
            Files.newDirectoryStream(f.toPath(), "*.pdf")
                    .forEach((path)->pdfSlice(path.toFile()));
        }
        catch (IOException e) {
            System.out.println("IOException *** " + e.getMessage());
        }
    }
    private static void pdfSlice(File f) {
        try {
            PDDocument pdd = Loader.loadPDF(f);
            PDFRenderer r = new PDFRenderer(pdd);
            File dirImages = mkdirImages(f);
            int pcount = pdd.getNumberOfPages();
            for (int pageno=1; pageno<=pcount; pageno++) {
                String[] orderTexts = pageTextSlice(getPageText(pdd,pageno));
                if(orderTexts.length == 0) continue;
                System.out.println(" " + orderTexts.length + " orders on page# " + pageno);
                pageImageSlice(r, pageno, orderTexts, dirImages);
            }
            System.out.println("" + pcount + " pages scanned.");
        }
        catch (IOException e) {
            System.out.println("IOException *** " + e.getMessage());
        }
    }
    private static File mkdirImages(File pdf) {
        String s = pdf.getName();
        File d = new File(pdf.getParentFile(), s.substring(0, s.lastIndexOf('.')));
        d.mkdir(); // create destination dir
        return d;
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
    private static void pageImageSlice(PDFRenderer r
            , int pageno
            , String[] orderTexts
            , File dirImages) throws IOException {
        BufferedImage image = r.renderImage(pageno - 1, IMAGE_SCALE);
        switch(orderTexts.length) {
            case 1:
                ImageIO.write(image
                    , "JPEG"
                    , new File(dirImages, name(orderTexts[0])+"p"+pageno+".jpg")
                );
                break;
            case 2:
                int h = image.getHeight();
                int w = image.getWidth();
                ImageIO.write(image.getSubimage(0, 0, w, h / 2)
                    , "JPEG"
                    , new File(dirImages, name(orderTexts[0])+"t"+pageno+".jpg")
                );
                ImageIO.write(image.getSubimage(0, h / 2, w, h - h / 2)
                    , "JPEG"
                    , new File(dirImages, name(orderTexts[1])+"b"+pageno+".jpg")
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
