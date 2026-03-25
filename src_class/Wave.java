package application;

// Her bir düşman dalgasını (wave) temsil eden sınıf
public class Wave {
    public int enemyCount;         // Dalgada üretilecek toplam düşman sayısı
    public double spawnInterval;   // Düşmanlar arasındaki doğma süresi (saniye cinsinden)
    public double delayBeforeStart; // Bu dalga başlamadan önce beklenecek süre (saniye cinsinden)

    // Yapıcı metod (constructor): bir dalga oluşturur
    public Wave(int enemyCount, double spawnInterval, double delayBeforeStart) {
        this.enemyCount = enemyCount;               // Düşman sayısını ayarla
        this.spawnInterval = spawnInterval;         // Doğma aralığını ayarla
        this.delayBeforeStart = delayBeforeStart;   // Başlamadan önceki gecikmeyi ayarla
    }
}
