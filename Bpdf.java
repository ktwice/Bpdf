//package bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        if(orders.length == 2)
            return ImageIO.write(image, "JPEG"
                , new File(dir, name(orders[0]) + "p" + pno + ".jpg")
            );
        int h = image.getHeight();
        int w = image.getWidth();
        return ImageIO.write(image.getSubimage(0, 0, w, h / 2), "JPEG"
            , new File(dir, name(orders[0]) + "t" + pno + ".jpg")
        ) && ImageIO.write(image.getSubimage(0, h / 2, w, h - h / 2), "JPEG"
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
}
