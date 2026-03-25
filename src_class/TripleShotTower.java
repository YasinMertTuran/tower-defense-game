package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Üç hedefe aynı anda ateş eden özel bir kule türü
public class TripleShotTower extends Tower {
    private Image image; // Kuleye ait görsel
    private List<Projectile> projectiles = new ArrayList<>(); // Aktif mermilerin listesi
    private long lastShotTime = 0; // Son ateş etme zamanı
    private long fireDelay = 700; // İki atış arası minimum süre (ms cinsinden)

    // Kurucu metod: kule konumu, menzili ve maliyeti tanımlanır
    public TripleShotTower(double x, double y) {
        super(x, y, 200, 150); // Menzil: 200 birim, maliyet: 150 birim
        image = new Image(getClass().getResourceAsStream("/triple_tower.png")); // Görsel yüklenir
    }

    // Her oyun güncellemesinde kule mantığı çalıştırılır
    @Override
    public void update(List<Main.MovingObject> enemies) {
        long now = System.currentTimeMillis();

        // Eğer yeterli süre geçmediyse yeni atış yapma
        if (now - lastShotTime < fireDelay) return;

        // Aktif olmayan mermileri listeden çıkar
        projectiles.removeIf(p -> !p.active);

        // Menzil içindeki en yakın 3 düşmanı bul, sırala ve her birine ateş et
        enemies.stream()
               .filter(e -> e.active && isInRange(e)) // Sadece aktif ve menzilde olan düşmanlar
               .sorted(Comparator.comparingDouble(this::distanceTo)) // Mesafeye göre sırala
               .limit(3) // İlk 3 düşmanı seç
               .forEach(e -> {
                   projectiles.add(new Projectile(x, y, e.posX, e.posY)); // Her düşman için mermi oluştur
                   e.takeDamage(15); // Her düşmana 15 birim hasar ver
               });

        // Atış zamanını güncelle
        lastShotTime = now;
    }

    // Kule ve mermilerini çizer
    @Override
    public void draw(GraphicsContext gc) {
        // Kule resmini çiz (ortalanmış şekilde)
        gc.drawImage(image, x - 25, y - 25, 50, 50);

        // Tüm mermileri güncelle ve çiz
        for (Projectile p : projectiles) {
            p.update();
            p.draw(gc);
        }

        // Eğer menzil gösterimi açıksa menzili kırmızı çemberle çiz
        if (showRange) {
            gc.setStroke(Color.RED);
            gc.strokeOval(x - range, y - range, range * 2, range * 2);
        }
    }

    // İç sınıf: Mermiyi temsil eder
    private class Projectile {
        double x, y;               // Merminin anlık konumu
        double targetX, targetY;   // Hedef pozisyonu
        double speed = 8;          // Mermi hızı
        boolean active = true;     // Mermi hala aktif mi (hedefe ulaşmadıysa true)

        // Mermi oluşturucu: hedefe doğru yönlendirilir
        public Projectile(double x, double y, double targetX, double targetY) {
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        // Mermi hedefe doğru hareket eder, hedefe ulaşırsa aktifliği biter
        public void update() {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < speed) {
                active = false; // Hedefe ulaştıysa mermiyi pasifleştir
            } else {
                // Hedef yönünde ilerlet
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        // Mermiyi grafiksel olarak çizer
        public void draw(GraphicsContext gc) {
            gc.setFill(Color.RED); // İç renk
            gc.fillOval(x - 6, y - 6, 12, 12);
            gc.setStroke(Color.DARKRED); // Dış çerçeve
            gc.strokeOval(x - 6, y - 6, 12, 12);
        }
    }
}
