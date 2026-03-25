package application;

import javafx.scene.canvas.GraphicsContext;

// Tüm kulelerin temelini oluşturan soyut sınıf
public abstract class Tower {
    public double x, y;           // Kule konumu (merkez noktası)
    public double range;         // Kule menzili (etki alanı yarıçapı)
    public int cost;             // Kule yerleştirme maliyeti
    public boolean selected = false;    // Kule seçili mi (UI için)
    public boolean showRange = false;   // Menzil görsel olarak gösterilsin mi

    // Kule oluşturucu (pozisyon, menzil ve maliyet atanır)
    public Tower(double x, double y, double range, int cost) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.cost = cost;
    }

    // Kule ile bir düşman arasındaki mesafeyi hesaplar
    public double distanceTo(Main.MovingObject enemy) {
        double dx = x - enemy.posX;
        double dy = y - enemy.posY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Belirli bir nokta (örneğin fare tıklaması), kulenin üstüne mi geliyor?
    public boolean contains(double px, double py) {
        return Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2)) <= 25;
    }

    // Belirli bir düşman, kulenin menzili içinde mi?
    public boolean isInRange(Main.MovingObject enemy) {
        return Math.sqrt(Math.pow(enemy.posX - x, 2) + Math.pow(enemy.posY - y, 2)) <= range;
    }

    // Soyut metot: her alt sınıf kendi güncelleme (mantık) kodunu sağlar
    public abstract void update(java.util.List<Main.MovingObject> enemies);

    // Soyut metot: her alt sınıf kendi çizim kodunu sağlar
    public abstract void draw(GraphicsContext gc);
}
