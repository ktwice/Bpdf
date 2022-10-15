//package bpdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * buh-pdf slicer
 * @author ktwice
 */
public class Bpdf {
    final static float IMAGE_SCALE = 7; // image quality. 1 = 72 DPI  
    final static Pattern pOrderSeparator = Pattern.compile("[^\\d]\\d{40}");
    final static Pattern pLineSeparator = Pattern.compile("\n+");
    final static Pattern pWordSeparator = Pattern.compile("[^\\d\\p{L}]+");
    private static String[] pageorders(final String page) {
        String[] orders = pOrderSeparator.split(page);
        return Arrays.copyOf(orders, orders.length - 1);
    }
    private static String name(final String order) {
        String sline = pLineSeparator.splitAsStream(order)
            .filter((line)->line.contains("Плательщик"))
            .findFirst().orElse("");
        return pWordSeparator.splitAsStream(sline)
            .reduce("", (x, w)->w.isEmpty() ? x : x.isEmpty() ? w : x + "-" + w);
    }

    public static void main(String[] args) throws IOException {
        File f = new File(args[0]);
        if(!f.isDirectory())
            pdfslicer(f);
        else try (DirectoryStream<Path> paths
                = Files.newDirectoryStream(f.toPath(), "*.pdf")) {
            for(Path path: paths)
                pdfslicer(path.toFile());
        }
    }
    private static void pdfslicer(File f) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        try(PDDocument pdd = Loader.loadPDF(f);) {

        String s = f.getName();
        File d = new File(f.getParentFile(), s.substring(0, s.lastIndexOf('.')));
        d.mkdir(); // destination dir

        PDFRenderer r = new PDFRenderer(pdd);
        int pcount = pdd.getNumberOfPages();
        for (int pno=1; pno<=pcount; pno++) {
            stripper.setStartPage(pno);
            stripper.setEndPage(pno);
            String[] orders = pageorders(stripper.getText(pdd));
            int ocount = orders.length;
            System.out.println(" " + ocount + " orders on page# " + pno);
            BufferedImage image = r.renderImage(pno - 1, IMAGE_SCALE);
            switch(ocount) {
                case 2:
                    int h2 = image.getHeight() / 2;
                    int w = image.getWidth();
                    ImageIO.write(image.getSubimage(0, 0, w, h2), "JPEG"
                            , new File(d, name(orders[0])+"-t"+pno+".jpg")
                    );
                    ImageIO.write(image.getSubimage(0, h2, w, h2), "JPEG"
                            , new File(d, name(orders[1])+"-b"+pno+".jpg")
                    );
                    break;
                case 1:
                    ImageIO.write(image, "JPEG"
                            , new File(d, name(orders[0])+"-p"+pno+".jpg")
                    );
                    break;
            }
        }
        System.out.println("" + pcount + " pages scanned.");
        }
    }
}
