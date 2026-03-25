package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

// LaserTower sınıfı, bir tür savunma kulesidir ve Tower sınıfından türetilmiştir.
public class LaserTower extends Tower {
    private Image image; // Kuleyi temsil eden görsel.
    private List<LaserBeam> laserBeams = new ArrayList<>(); // Gönderilen lazer ışınlarını tutar.

    // LaserTower yapıcısı (constructor) – konum ayarlanır ve resim yüklenir.
    public LaserTower(double x, double y) {
        super(x, y, 200, 120); // Tower sınıfının yapıcısı çağrılır (x, y, menzil, atış hızı).
        image = new Image(getClass().getResourceAsStream("/laser_tower.png")); // Görsel yüklenir.
    }

    // Her güncelleme döngüsünde düşmanlara saldırmak için çağrılır.
    @Override
    public void update(java.util.List<Main.MovingObject> enemies) {
        laserBeams.clear(); // Önceki lazer ışınlarını temizle.
        for (Main.MovingObject enemy : enemies) {
            if (enemy.active && isInRange(enemy)) { // Eğer düşman aktifse ve menzildeyse:
                enemy.takeDamage(1); // Düşmana hasar ver.
                // Lazer ışını oluştur ve listeye ekle.
                laserBeams.add(new LaserBeam(x, y, enemy.posX, enemy.posY));
            }
        }
    }

    // Kule ve lazer ışınlarını ekrana çizen metod.
    @Override
    public void draw(GraphicsContext gc) {
        // Kule görselini çiz (merkez noktaya göre ortalanmış şekilde).
        gc.drawImage(image, x - 25, y - 25, 50, 50);

        // Tüm lazer ışınlarını çiz.
        for (LaserBeam beam : laserBeams) {
            beam.draw(gc);
        }

        // Eğer menzil gösterimi açıksa, menzili kırmızı çemberle göster.
        if (showRange) {
            gc.setStroke(Color.RED);
            gc.strokeOval(x - range, y - range, range * 2, range * 2);
        }
    }

    // İç sınıf: lazer ışınını temsil eder.
    private class LaserBeam {
        double x1, y1, x2, y2; // Başlangıç ve bitiş koordinatları

        public LaserBeam(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        // Lazer ışınını çizen metod.
        public void draw(GraphicsContext gc) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeLine(x1, y1, x2, y2); // Kırmızı çizgi ile lazeri çiz.
        }
    }
}
