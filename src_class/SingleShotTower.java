package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

// Tekli atış yapan kule sınıfı (Tower sınıfından türetilmiştir)
public class SingleShotTower extends Tower {
    private Image image; // Kuleye ait görsel (resim)
    private List<Projectile> projectiles = new ArrayList<>(); // Ateşlenen mermilerin listesi
    private long lastShotTime = 0; // Son ateş etme zamanı
    private long fireDelay = 500; // Her ateş arasında minimum bekleme süresi (milisaniye)

    // Kurucu: konum bilgisi alır, menzil ve maliyet belirler
    public SingleShotTower(double x, double y) {
        super(x, y, 200, 50); // menzil: 200 birim, maliyet: 50 birim
        image = new Image(getClass().getResourceAsStream("/single_tower.png")); // Resmi yükle
    }

    // Her oyun döngüsünde kule güncellenir
    @Override
    public void update(List<Main.MovingObject> enemies) {
        long now = System.currentTimeMillis();

        // Eğer atış gecikme süresi dolmamışsa hiçbir şey yapma
        if (now - lastShotTime < fireDelay) return;

        // Aktif olmayan mermileri listeden çıkar
        projectiles.removeIf(p -> !p.active);

        // Menzildeki en yakın düşmanı bul
        Main.MovingObject target = null;
        double minDist = Double.MAX_VALUE;

        for (Main.MovingObject enemy : enemies) {
            if (enemy.active && isInRange(enemy)) {
                double dist = distanceTo(enemy);
                if (dist < minDist) {
                    minDist = dist;
                    target = enemy; // En yakın hedef olarak ata
                }
            }
        }

        // Hedef varsa mermi oluştur, hedefe ateş et ve hasar ver
        if (target != null) {
            projectiles.add(new Projectile(x, y, target.posX, target.posY)); // Mermi oluştur
            target.takeDamage(10); // Hedefe 10 birim hasar ver
            lastShotTime = now; // Son atış zamanı güncelle
        }
    }

    // Kule ve mermilerini çizen metod
    @Override
    public void draw(GraphicsContext gc) {
        // Kuleyi çiz
        gc.drawImage(image, x - 25, y - 25, 50, 50); // Kule resmi (merkeze göre çizilir)

        // Mermileri güncelle ve çiz
        for (Projectile p : projectiles) {
            p.update();
            p.draw(gc);
        }

        // Eğer menzil görünür yapılmışsa menzil çemberini çiz
        if (showRange) {
            gc.setStroke(Color.RED);
            gc.strokeOval(x - range, y - range, range * 2, range * 2);
        }
    }

    // İç sınıf: Kuleye ait mermi sınıfı
    private class Projectile {
        double x, y, targetX, targetY; // Merminin mevcut ve hedef konumu
        double speed = 12; // Mermi hızı (her karede ilerleme miktarı)
        boolean active = true; // Mermi hala aktif mi?

        // Mermi oluşturucu
        public Projectile(double x, double y, double targetX, double targetY) {
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        // Merminin hareketi güncellenir
        public void update() {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Hedefe çok yakınsa artık aktif değil
            if (distance < speed) {
                active = false;
            } else {
                // Hedefe doğru hareket et
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        // Mermiyi çizen metod (kırmızı iç, turuncu kenar)
        public void draw(GraphicsContext gc) {
            gc.setFill(Color.RED);
            gc.fillOval(x - 4, y - 4, 8, 8); // Mermiyi ortalayarak daire çiz
            gc.setStroke(Color.ORANGE);
            gc.strokeOval(x - 4, y - 4, 8, 8);
        }
    }
}
