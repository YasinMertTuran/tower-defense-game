package application;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Füze fırlatan bir kuleyi temsil eden sınıf. Tower sınıfından türetilmiştir.
public class MissileLauncherTower extends Tower {
    private Image image; // Kule görüntüsü
    private final List<Missile> missiles = new ArrayList<>(); // Aktif füzelerin listesi
    private long lastShotTime = 0; // Son atış zamanı
    private final long fireDelay = 1500; // Atışlar arası gecikme süresi (milisaniye cinsinden)
    private final double explosionRadius = 70; // Patlama yarıçapı
    private final int damage = 35; // Patlamadaki hasar miktarı

    // Kurucu metod: Konum, menzil ve fiyat belirlenir. Görüntü yüklenir.
    public MissileLauncherTower(double x, double y) {
        super(x, y, 220, 200); // menzil 220, fiyat 200
        image = new Image(getClass().getResourceAsStream("/missile_tower.png"));
    }

    // Her karede (frame) kule güncellenir: atış yapar ve füzeleri günceller.
    @Override
    public void update(List<Main.MovingObject> enemies) {
        long now = System.currentTimeMillis();

        // Belirtilen sürede bir kez atış yapılır
        if (now - lastShotTime >= fireDelay) {
            Main.MovingObject target = findClosestEnemy(enemies); // En yakın hedef belirlenir
            if (target != null) {
                // Füze oluşturulup listeye eklenir
                missiles.add(new Missile(x, y, target.posX, target.posY));
                lastShotTime = now; // Son atış zamanı güncellenir
            }
        }

        // Mevcut füzeler güncellenir
        updateMissiles(enemies);
    }

    // Menzildeki en yakın düşmanı bulur
    private Main.MovingObject findClosestEnemy(List<Main.MovingObject> enemies) {
        Main.MovingObject closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Main.MovingObject enemy : enemies) {
            if (!enemy.active) continue;

            double dist = distanceTo(enemy);
            if (dist < range && dist < minDistance) {
                minDistance = dist;
                closest = enemy;
            }
        }
        return closest;
    }

    // Füzelerin konumu ve patlama durumu güncellenir
    private void updateMissiles(List<Main.MovingObject> enemies) {
        Iterator<Missile> iterator = missiles.iterator();
        while (iterator.hasNext()) {
            Missile missile = iterator.next();

            if (!missile.exploded) {
                missile.updatePosition(); // Füze hareket ettirilir
                if (missile.reachedTarget()) {
                    missile.explode(); // Hedefe ulaştıysa patlatılır
                    applyExplosionDamage(missile, enemies); // Patlama hasarı uygulanır
                }
            } else if (System.currentTimeMillis() - missile.explosionTime > 300) {
                // Patlamadan 300 ms sonra füze silinir
                iterator.remove();
            }
        }
    }

    // Patlama yarıçapındaki düşmanlara hasar verir
    private void applyExplosionDamage(Missile missile, List<Main.MovingObject> enemies) {
        for (Main.MovingObject enemy : enemies) {
            if (!enemy.active) continue;

            double dist = Math.hypot(enemy.posX - missile.x, enemy.posY - missile.y);
            if (dist <= explosionRadius) {
                enemy.takeDamage(damage);
            }
        }
    }

    // Kuleyi ve aktif füzeleri çizer
    @Override
    public void draw(GraphicsContext gc) {
        // Kule görselini çiz
        gc.drawImage(image, x - 25, y - 25, 50, 50);

        // Tüm füzeleri çiz
        for (Missile missile : missiles) {
            missile.draw(gc);
        }

        // Menzil çemberi (isteğe bağlı gösterim)
        if (showRange) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            gc.strokeOval(x - range, y - range, range * 2, range * 2);
        }
    }

    // İç sınıf: Füze objesini temsil eder
    private class Missile {
        double x, y; // Füzenin mevcut konumu
        final double targetX, targetY; // Hedef koordinatları
        final double speed = 6.0; // Füze hızı
        boolean exploded = false; // Patlayıp patlamadığı
        long explosionTime = 0; // Patlama zamanı

        // Füze oluşturulurken başlangıç ve hedef konumu belirlenir
        public Missile(double startX, double startY, double endX, double endY) {
            this.x = startX;
            this.y = startY;
            this.targetX = endX;
            this.targetY = endY;
        }

        // Füzenin konumu güncellenir (hedefe doğru hareket)
        public void updatePosition() {
            if (exploded) return;

            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.hypot(dx, dy);

            if (distance > 0) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        // Füze hedefe yeterince yaklaştı mı kontrol edilir
        public boolean reachedTarget() {
            return Math.hypot(targetX - x, targetY - y) < 10;
        }

        // Füze patlatılır
        public void explode() {
            exploded = true;
            explosionTime = System.currentTimeMillis();
        }

        // Füzenin çizimi yapılır (hareket veya patlama)
        public void draw(GraphicsContext gc) {
            if (exploded) {
                // Patlama efekti
                gc.setFill(Color.rgb(255, 100, 0, 0.5)); // Dış halka
                gc.fillOval(x - explosionRadius, y - explosionRadius, explosionRadius * 2, explosionRadius * 2);

                gc.setFill(Color.rgb(255, 200, 0, 0.8)); // İç halka
                gc.fillOval(x - explosionRadius / 2, y - explosionRadius / 2, explosionRadius, explosionRadius);
            } else {
                // Füze çizimi
                gc.setFill(Color.DARKRED);
                gc.fillOval(x - 8, y - 8, 16, 16); // Füze gövdesi

                // Füzenin yönünü gösteren bir çizgi
                gc.setStroke(Color.ORANGE);
                double angle = Math.atan2(targetY - y, targetX - x);
                gc.strokeLine(x, y, x - 15 * Math.cos(angle), y - 15 * Math.sin(angle));
            }
        }
    }
}
