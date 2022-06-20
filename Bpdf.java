//package bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * buh-pdf slicer
 * @author k2-adm
 */
public class Bpdf {
    final static float IMAGE_SCALE = 7; // image quality. 1 = 72 DPI  
    final static Pattern pOrderSeparator = Pattern.compile("[^\\d]\\d{40}");
    final static Pattern pLineSeparator = Pattern.compile("\n+");
    final static Pattern pWordSeparator = Pattern.compile("[^\\d\\p{L}]+");

    public static void main(String[] args) throws IOException {
        File f0 = new File(args[0]);
        if(!f0.isDirectory()) {
            pdfslicer(f0);
            return;
        }
        for(String s:f0.list()) {
            int slen = s.length();
            if(slen <= 4) continue;
            String sext = s.substring(slen-4, slen);
            if(!".pdf".equals(sext.toLowerCase())) continue;
            pdfslicer(new File(f0, s));
        }
    }
    private static File pdf2d(File f) {
        String s = f.getName();
        File d = new File(f.getParentFile(), s.substring(0, s.length() - 4));
        d.mkdir();
        return d;
    }
    private static void pdfslicer(File f) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        try(PDDocument pdd = Loader.loadPDF(f);) {
        File d = pdf2d(f);
        PDFRenderer r = new PDFRenderer(pdd);
        int pcount = pdd.getNumberOfPages();
        for (int pno=1; pno<=pcount; pno++) {
            stripper.setStartPage(pno);
            stripper.setEndPage(pno);
            String pageText = stripper.getText(pdd);
            String[] orders = pOrderSeparator.split(pageText);
            int ocount = orders.length - 1;
            System.out.println(" " + ocount + " orders on page# " + pno);
            BufferedImage image = r.renderImage(pno - 1, IMAGE_SCALE);
            if(ocount == 2) {
                int h2 = image.getHeight() / 2;
                int w = image.getWidth();
                ImageIO.write(image.getSubimage(0, 0, w, h2), "JPEG"
                        , new File(d, name(orders[0])+"-t"+pno+".jpg")
                );
                ImageIO.write(image.getSubimage(0, h2, w, h2), "JPEG"
                        , new File(d, name(orders[1])+"-b"+pno+".jpg")
                );
            } else if(ocount == 1) {
                ImageIO.write(image, "JPEG"
                        , new File(d, name(orders[0])+"-p"+pno+".jpg")
                );
            }
        }
        System.out.println("" + pcount + " pages scanned.");
        }
    }
    private static String name(final String order) {
        String sline = pLineSeparator.splitAsStream(order)
                .filter((line)->line.contains("Плательщик"))
                .findFirst().orElse("");
        String sname = pWordSeparator.splitAsStream(sline)
                .reduce("", (x, w)->w.isEmpty() ? x : x.isEmpty() ? w : x + "-" + w);
        return sname;
    }
}
