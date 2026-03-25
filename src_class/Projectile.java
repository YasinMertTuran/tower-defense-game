package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// Bu sınıf, bir mermiyi temsil eder ve hareket/görsel çizim gibi işlevleri içerir.
public class Projectile {
    double x, y; // Merminin mevcut konumu
    double targetX, targetY; // Merminin hedef konumu
    double speed = 8; // Merminin hızı (her karede ilerleme miktarı)
    boolean active = true; // Mermi aktif mi? (hedefine ulaştığında false olur)
    double size = 6; // Merminin çapı (çizim için)

    // Yapıcı metod: Mermiyi oluşturur ve başlangıç/target koordinatlarını alır
    public Projectile(double x, double y, double targetX, double targetY) {
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // Mermiyi her karede günceller: hedefe doğru ilerletir
    public void update() {
        double dx = targetX - x; // Hedefe yatay mesafe
        double dy = targetY - y; // Hedefe dikey mesafe
        double distance = Math.sqrt(dx * dx + dy * dy); // Hedefe olan toplam mesafe

        // Eğer mermi hedefe çok yakınsa artık hareket etmez (aktif değil)
        if (distance < speed) {
            active = false;
        } else {
            // Normalde hedefe doğru bir adım ilerler
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
    }

    // Mermiyi ekrana çizer (sarı içi dolu ve turuncu kenarlıklı daire olarak)
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - size/2, y - size/2, size, size); // Ortalanmış daire
        gc.setStroke(Color.ORANGE);
        gc.strokeOval(x - size/2, y - size/2, size, size);
    }

    // Mermi hâlâ aktif mi? (dışarıdan kontrol için)
    public boolean isActive() {
        return active;
    }
}
