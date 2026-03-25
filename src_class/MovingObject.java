package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// Oyun alanında hareket eden nesneyi (örneğin düşmanı) temsil eder
public class MovingObject {
    public double posX, posY; // Nesnenin konumu
    public int currentTargetIndex = 1; // Hedef noktalar arasında hangi noktaya gittiğini tutar
    public boolean active = true; // Nesne hala hayattaysa true
    public double speed; // Hareket hızı
    public int health = 30; // Can değeri
    public double radius = 20; // Çizimdeki yarıçapı (görsel boyutu)

    // Yapıcı metod: nesne oluşturulurken hızı belirlenir
    public MovingObject(double speed) {
        this.speed = speed;
        this.health = 30; // Başlangıç can değeri
    }

    // Nesnenin konumu güncellenir (hareket ettirilir)
    public void update(double deltaTime) {
        // Şu an için sadece sağa doğru sabit hızla ilerliyor
        posX += speed * deltaTime;

    }

    // Hasar alma işlevi: Can azaltılır, 0 veya altına inerse nesne pasif yapılır
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            active = false;
        }
    }

    // Nesne ekrana çizilir
    public void draw(GraphicsContext gc) {
        if (!active) return; // aktif değilse çizme

        gc.setFill(Color.RED); // Renk kırmızı olarak ayarlanır
        gc.fillOval(posX - radius, posY - radius, radius * 2, radius * 2); // Daire olarak çizilir
    }
}
