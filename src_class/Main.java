package application;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main extends Application {
    // Oyun alanı boyutları ve hücre özellikleri
    private int CELL_SIZE = 60; // Her bir hücrenin piksel cinsinden boyutu
    private int ROWS = 10;      // Oyun alanındaki satır sayısı
    private int COLS = 10;      // Oyun alanındaki sütun sayısı
    
    // Oyun durumu değişkenleri
    private int waveIndex = 0;       // Mevcut dalga indeksi
    private int money = 100;         // Oyuncunun parası
    private int lives = 5;           // Oyuncunun kalan canı
    private int currentLevel = 1;    // Mevcut seviye
    
    // Dalga yönetimi için zamanlayıcılar
    private Timeline nextWaveTimer;      // Sonraki dalga için geri sayım zamanlayıcısı
    private double nextWaveCountdown;    // Sonraki dalga için kalan süre

    // Oyun nesneleri ve veri yapıları
    private Set<String> orangeCells = new HashSet<>(); // Turuncu hücrelerin koordinatları
    private List<int[]> path = new ArrayList<>();      // Düşmanların izleyeceği yol
    private List<Wave> waves = new ArrayList<>();       // Dalga bilgileri
    private List<MovingObject> objects = new ArrayList<>(); // Hareketli nesneler (düşmanlar)
    private List<Tower> towers = new ArrayList<>();     // Oyuncunun kuleleri
    
    // UI bileşenleri
    private Label moneyLabel;       // Para göstergesi
    private Label livesLabel;       // Can göstergesi
    private Label nextWaveLabel;    // Sonraki dalga bilgisi
    
    // Oyun döngüsü
    private AnimationTimer gameLoop; // Ana oyun döngüsü
    private long lastFrameTime = 0;  // Son kare zamanı

    // Dalga yönetimi
    private int currentWaveIndex = 0;           // Mevcut dalga indeksi
    private int enemiesSpawnedInWave = 0;       // Dalgada çıkan düşman sayısı
    private long lastSpawnTime = 0;             // Son düşman çıkış zamanı
    private boolean waitingForNextWave = false; // Sonraki dalga bekleniyor mu
    
    // Kule seçim ve sürükleme
    private Tower selectedTower = null;    // Seçili kule
    private boolean draggingTower = false; // Kule sürükleniyor mu
    private Tower previewTower = null;    // Önizleme kulesi
    
    // Diğer privateler
    private Timeline waveTimeline = new Timeline(); // Dalga zaman çizelgesi
    private Map<String, Double> cellOpacities = new HashMap<>(); // Hücre opaklıkları

    // Hareketli nesne (düşman) sınıfı
    public class MovingObject {
        double posX, posY;            // Düşmanın konumu
        int currentTargetIndex = 1;   // Mevcut hedef yol noktası indeksi
        boolean active = true;        // Düşman aktif mi
        double speed = 1.5;           // Düşman hızı
        int health = 30;              // Düşman canı
        double radius = 20;           // Düşman çapı

        MovingObject() {
            resetPosition(); // Başlangıç pozisyonunu ayarla
        }
        
        // Düşman can çubuğunu çiz
        void drawHealthBar(GraphicsContext gc) {
            double barWidth = 40;
            double barHeight = 5;
            double barX = posX - barWidth / 2;
            double barY = posY - 30;
            
            // Can yüzdesine göre çubuk uzunluğunu hesapla
            double healthRatio = (double) health / 30;
            gc.setFill(Color.GREEN);
            gc.fillRect(barX, barY, barWidth * healthRatio, barHeight);
        }

        // Düşman pozisyonunu sıfırla
        void resetPosition() {
            if (!path.isEmpty()) {
                // Yolun başlangıç noktasına yerleştir
                posX = path.get(0)[1] * CELL_SIZE + CELL_SIZE / 2.0;
                posY = path.get(0)[0] * CELL_SIZE + CELL_SIZE / 2.0;
            }
            currentTargetIndex = 1;
            active = true;
        }

        // Düşmana hasar ver
        void takeDamage(int damage) {
            health -= damage;
            if (health <= 0) {
                active = false;
                money += 10; // Düşman öldüğünde para kazan
                moneyLabel.setText("Money: $" + money);
            }
        }

        // Düşmanı güncelle (hareket ettir)
        void update(double deltaTime) {
            if (!active || path.isEmpty() || currentTargetIndex >= path.size()) return;

            // Bir sonraki hedef noktayı al
            int[] target = path.get(currentTargetIndex);
            double targetX = target[1] * CELL_SIZE + CELL_SIZE / 2.0;
            double targetY = target[0] * CELL_SIZE + CELL_SIZE / 2.0;

            // Hedefe olan mesafeyi hesapla
            double pathDx = targetX - posX;
            double pathDy = targetY - posY;
            double pathDist = Math.sqrt(pathDx * pathDx + pathDy * pathDy);

            // Hedefe ulaşıldı mı kontrol et
            if (pathDist < speed * deltaTime) {
                posX = targetX;
                posY = targetY;
                if (currentTargetIndex == path.size() - 1) {
                    // Son noktaya ulaştı - can kaybı
                    active = false;
                    lives--;
                    livesLabel.setText("Lives: " + lives);
                    if (lives <= 0) {
                        gameOver(); // Oyun bitti
                    }
                } else {
                    currentTargetIndex++; // Sonraki hedefe geç
                }
                return;
            }

            // Hareket vektörünü hesapla ve konumu güncelle
            double totalDx = pathDx / pathDist;
            double totalDy = pathDy / pathDist;

            posX += totalDx * speed * deltaTime;
            posY += totalDy * speed * deltaTime;
        }

        // Düşmanı çiz
        void draw(GraphicsContext gc) {
            if (!active) return;
            Image enemyImage = new Image(getClass().getResourceAsStream("/red_pawn.png"));
            gc.drawImage(enemyImage, posX - 20, posY - 20, 40, 40);
            drawHealthBar(gc); // Can çubuğunu çiz
        }
    }

    // Oyun bitti ekranı
    private void gameOver() {
        System.out.println("Game Over!");

        // JavaFX UI işlemleri için platform thread'inde çalış
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) livesLabel.getScene().getWindow();

            // Oyun bitti yazısı
            Label gameOverLabel = new Label("Game Over!");
            gameOverLabel.setStyle("-fx-text-fill: red; -fx-font-size: 60px; -fx-font-weight: bold;");

            // Ana menü butonu
            Button mainMenuButton = new Button("Main Menu");
            mainMenuButton.setStyle("-fx-font-size: 30px; -fx-background-color: rgb(255, 221, 158);");
            mainMenuButton.setOnAction(e -> showLevelSelect(stage));

            // Yeniden dene butonu
            Button retryButton = new Button("Retry");
            retryButton.setStyle("-fx-font-size: 30px; -fx-background-color: rgb(255, 221, 158);");
            retryButton.setOnAction(e -> startGame(stage, getCurrentLevel()));

            // UI düzenleme
            VBox vbox = new VBox(30, gameOverLabel, retryButton, mainMenuButton);
            vbox.setStyle("-fx-alignment: center;");
            Scene gameOverScene = new Scene(vbox, 1600, 800);
            stage.setScene(gameOverScene);
        });
        gameLoop.stop(); // Oyun döngüsünü durdur
    }

    // Yeni düşman oluştur
    private void spawnEnemy() {
        MovingObject enemy = new MovingObject();
        objects.add(enemy);
    }
    
    // Kazanma durumunu kontrol et
    private void checkWinCondition(Stage stage, int currentLevel) {
        // Tüm düşmanlar öldürüldü mü ve tüm dalgalar tamamlandı mı kontrol et
        boolean allEnemiesDefeated = objects.stream().allMatch(e -> !e.active);
        boolean allWavesSpawned = waveIndex >= waves.size();

        if (allEnemiesDefeated && allWavesSpawned) {
            gameLoop.stop();
            // Kazanma ekranı
            Label winLabel = new Label("You Win!");
            winLabel.setStyle("-fx-text-fill: red; -fx-font-size: 60px; -fx-font-weight: bold;");

            // Ana menü butonu
            Button mainMenuButton = new Button("Main Menu");
            mainMenuButton.setStyle("-fx-font-size: 30px; -fx-background-color: rgb(255, 221, 158);");
            mainMenuButton.setOnAction(e -> showLevelSelect(stage));

            // Sonraki seviye butonu
            Button nextLevelButton = new Button("Next Level");
            nextLevelButton.setStyle("-fx-font-size: 30px; -fx-background-color: rgb(255, 221, 158);");
            nextLevelButton.setOnAction(e -> startGame(stage, currentLevel + 1));

            // UI düzenleme
            VBox vbox = new VBox(30, winLabel, nextLevelButton, mainMenuButton);
            vbox.setStyle("-fx-alignment: center;");
            Scene winScene = new Scene(vbox, 1600, 800);
            stage.setScene(winScene);
        }
    }
    
    // Oyun durumunu sıfırla
    private void resetGameState() {
        waveIndex = 0;
        money = 100;
        lives = 5;
        objects.clear();
        towers.clear();
        path.clear();
        waves.clear();
        cellOpacities.clear();
        selectedTower = null;
        draggingTower = false;
        previewTower = null;
    }
    
    // Mevcut seviyeyi getir
    private int getCurrentLevel() {
        return currentLevel;
    }

    // Uygulama başlangıç noktası
    @Override
    public void start(Stage stage) {
        showMainMenu(stage); // Ana menüyü göster
    }

    // Ana menüyü göster
    private void showMainMenu(Stage stage) {
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-background-color: rgb(255, 221, 158); -fx-text-fill: black;"
                + " -fx-font-size: 30px; -fx-font-weight: normal;-fx-padding: 60px 100px 60px 100px;");
        startButton.setOnAction(e -> showLevelSelect(stage));
        StackPane root = new StackPane(startButton);
        Scene scene = new Scene(root, 1600, 800);
        stage.setScene(scene);
        stage.setTitle("Tower Defense");
        stage.show();
    }

    // Seviye seçim ekranını göster
    private void showLevelSelect(Stage stage) {
        VBox vbox = new VBox(20);
        vbox.setStyle("-fx-alignment: center;");

        // 5 seviye için butonlar oluştur
        for (int i = 1; i <= 5; i++) {
            int levelNumber = i;
            Button levelButton = new Button("Level " + levelNumber);
            levelButton.setStyle("-fx-background-color: rgb(255, 221, 158); -fx-text-fill: black;"
                    + " -fx-font-size: 30px; -fx-font-weight: normal;-fx-padding: 20px 40px 20px 40px;");
            levelButton.setOnAction(e -> startGame(stage, levelNumber));
            vbox.getChildren().add(levelButton);
        }

        Scene scene = new Scene(vbox, 1600, 800);
        stage.setScene(scene);
        stage.setTitle("Select Level");
    }
    
    // Oyunu başlat
    private void startGame(Stage stage, int level) {
        lastFrameTime = 0;
        resetGameState();
        loadLevelFromFile("level" + level + ".txt"); // Seviye dosyasını yükle
        currentLevel = level;

        // Seviyeye göre başlangıç parasını ayarla
        switch (currentLevel) {
            case 1:
                money = 100;
                break;
            case 2:
                money = 120;
                break;
            case 3:
                money = 190;
                break;
            case 4:
                money = 490;
                break;
            case 5:
                money = 620;
                break;
            default:
                money = 100; // Beklenmedik bir seviye için varsayılan
                break;
        }

        // Oyun alanını oluştur
        Canvas canvas = new Canvas(COLS * CELL_SIZE, ROWS * CELL_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setLayoutX((1600-canvas.getWidth())/2);
        canvas.setLayoutY(100);
        
        // Para göstergesi
        moneyLabel = new Label("Money: $" + money);
        moneyLabel.setLayoutX(1330);
        moneyLabel.setLayoutY(60);
        moneyLabel.setStyle("-fx-text-fill: black; -fx-font-size: 30px; -fx-font-weight: normal;");
        
        // Can göstergesi
        livesLabel = new Label("Lives: " + lives);
        livesLabel.setLayoutX(1330);
        livesLabel.setLayoutY(20);
        livesLabel.setStyle("-fx-text-fill: black; -fx-font-size: 30px; -fx-font-weight: normal;");
        
        // Sonraki dalga bilgisi
        nextWaveLabel = new Label("Next Wave: ");
        nextWaveLabel.setLayoutX(1330);
        nextWaveLabel.setLayoutY(100);
        nextWaveLabel.setStyle("-fx-text-fill: black; -fx-font-size: 30px; -fx-font-weight: normal;");

        // Kule sürükleme olayları
        canvas.setOnDragOver(event -> {
            if (event.getGestureSource() != canvas && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);

                // Önizleme kulesi oluştur
                if (previewTower == null) {
                    String towerType = event.getDragboard().getString();
                    int cost = switch (towerType) {
                        case "LaserTower" -> 120;
                        case "MissileLauncherTower" -> 200;
                        case "TripleShotTower" -> 150;
                        default -> 50;
                    };
                    
                    // Yeterli para varsa önizleme kulesi oluştur
                    if (money >= cost) {
                        previewTower = switch (towerType) {
                            case "LaserTower" -> new LaserTower(0, 0);
                            case "MissileLauncherTower" -> new MissileLauncherTower(0, 0);
                            case "TripleShotTower" -> new TripleShotTower(0, 0);
                            default -> new SingleShotTower(0, 0);
                        };
                        previewTower.showRange = true; // Menzil göster
                    }
                }

                // Önizleme kulesini fare pozisyonuna taşı
                if (previewTower != null) {
                    previewTower.x = event.getX();
                    previewTower.y = event.getY();
                }
            }
            event.consume();
        });

        // Kule bırakma olayı
        canvas.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString() && previewTower != null) {
                String towerType = db.getString();
                int col = (int)(event.getX() / CELL_SIZE);
                int row = (int)(event.getY() / CELL_SIZE);

                // Bırakma konumu geçerli mi kontrol et
                boolean isPathCell = path.stream().anyMatch(p -> p[0] == row && p[1] == col);
                boolean isOccupied = towers.stream().anyMatch(t -> {
                    int towerRow = (int)(t.y / CELL_SIZE);
                    int towerCol = (int)(t.x / CELL_SIZE);
                    return towerRow == row && towerCol == col;
                });

                // Yol üzerinde veya başka bir kulenin üzerinde değilse
                if (!isPathCell && !isOccupied) {
                    double x = col * CELL_SIZE + CELL_SIZE / 2.0;
                    double y = row * CELL_SIZE + CELL_SIZE / 2.0;

                    // Kule tipine göre yeni kule oluştur
                    Tower tower = switch (towerType) {
                        case "LaserTower" -> {
                            if (money >= 120) {
                                money -= 120;
                                yield new LaserTower(x, y);
                            }
                            yield null;
                        }
                        case "MissileLauncherTower" -> {
                            if (money >= 200) {
                                money -= 200;
                                yield new MissileLauncherTower(x, y);
                            }
                            yield null;
                        }
                        case "TripleShotTower" -> {
                            if (money >= 150) {
                                money -= 150;
                                yield new TripleShotTower(x, y);
                            }
                            yield null;
                        }
                        default -> {
                            if (money >= 50) {
                                money -= 50;
                                yield new SingleShotTower(x, y);
                            }
                            yield null;
                        }
                    };

                    // Kuleyi ekle ve parayı güncelle
                    if (tower != null) {
                        towers.add(tower);
                        moneyLabel.setText("Money: $" + money);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
            previewTower = null; // Önizlemeyi temizle
        });

        // Kule seçme olayı
        canvas.setOnMousePressed(e -> {
            for (Tower tower : towers) {
                if (tower.contains(e.getX(), e.getY())) {
                    selectedTower = tower;
                    draggingTower = true;
                    tower.showRange = true; // Menzil göster
                    break;
                }
            }
        });

        // Kule sürükleme olayı
        canvas.setOnMouseDragged(event -> {
            if (draggingTower && selectedTower != null) {
                selectedTower.x = event.getX();
                selectedTower.y = event.getY();
                selectedTower.showRange = true;
            }
        });

        // Kule bırakma olayı
        canvas.setOnMouseReleased(event -> {
            if (draggingTower && selectedTower != null) {
                int col = (int)(event.getX() / CELL_SIZE);
                int row = (int)(event.getY() / CELL_SIZE);

                // Geçerli bir konum mu kontrol et
                boolean outOfBounds = col < 0 || col >= COLS || row < 0 || row >= ROWS;
                boolean onPath = path.stream().anyMatch(p -> p[0] == row && p[1] == col);

                // Aynı hücrede başka bir kule var mı kontrol et
                boolean occupiedByAnother = towers.stream().anyMatch(t -> {
                    if (t == selectedTower) return false; // Kendisiyle çakışma önemli değil
                    int tRow = (int)(t.y / CELL_SIZE);
                    int tCol = (int)(t.x / CELL_SIZE);
                    return tRow == row && tCol == col;
                });

                if (outOfBounds || onPath || occupiedByAnother) {
                    // Geçersiz konum - kuleyi sil ve parasını geri ver
                    towers.remove(selectedTower);
                    int refund = switch (selectedTower.getClass().getSimpleName()) {
                        case "LaserTower" -> 120;
                        case "MissileLauncherTower" -> 200;
                        case "TripleShotTower" -> 150;
                        case "SingleShotTower" -> 50;
                        default -> 0;
                    };
                    money += refund;
                    moneyLabel.setText("Money: $" + money);
                } else {
                    // Geçerli konuma yerleştir
                    selectedTower.x = col * CELL_SIZE + CELL_SIZE / 2.0;
                    selectedTower.y = row * CELL_SIZE + CELL_SIZE / 2.0;
                    selectedTower.showRange = false;
                }

                selectedTower = null;
                draggingTower = false;
            }
        });

        // Oyun döngüsü
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTime == 0) {
                    lastFrameTime = now;
                    return;
                }

                // Delta time hesapla (saniye cinsinden)
                double deltaTime = (now - lastFrameTime) / 10_000_000.0;
                lastFrameTime = now;

                // Ekranı temizle
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                drawGrid(gc); // Izgarayı çiz

                // Tüm nesneleri güncelle ve çiz
                for (MovingObject obj : objects) obj.update(deltaTime);
                objects.removeIf(e -> !e.active); // Aktif olmayanları sil
                for (MovingObject obj : objects) obj.draw(gc);

                // Kuleleri güncelle ve çiz
                for (Tower tower : towers) {
                    tower.update(objects);
                    tower.draw(gc);
                }

                // Önizleme kulesini çiz
                if (previewTower != null) {
                    previewTower.draw(gc);
                }

                // Kazanma durumunu kontrol et
                checkWinCondition((Stage) gc.getCanvas().getScene().getWindow(), level);
            }
        };
        gameLoop.start();

        startWaves(); // Dalgaları başlat

        // UI bileşenlerini oluştur
        Pane root = new Pane();
        root.getChildren().add(canvas);

        // Kule ikonlarını yükle
        Image laserTowerImage = new Image(getClass().getResourceAsStream("/laser_tower.png"));
        ImageView laserView = new ImageView(laserTowerImage);
        laserView.setFitWidth(60);
        laserView.setFitHeight(60);
        laserView.setLayoutX(1400);
        laserView.setLayoutY(300);
        laserView.setUserData("LaserTower");
        
        Image missileImage = new Image(getClass().getResourceAsStream("/missile_tower.png"));
        ImageView missileView = new ImageView(missileImage);
        missileView.setFitWidth(60);
        missileView.setFitHeight(60);
        missileView.setLayoutX(1400);
        missileView.setLayoutY(500);
        missileView.setUserData("MissileLauncherTower");

        Image singleImage = new Image(getClass().getResourceAsStream("/single_tower.png"));
        ImageView singleView = new ImageView(singleImage);
        singleView.setFitWidth(60);
        singleView.setFitHeight(60);
        singleView.setLayoutX(1400);
        singleView.setLayoutY(200);
        singleView.setUserData("SingleShotTower");

        Image tripleImage = new Image(getClass().getResourceAsStream("/triple_tower.png"));
        ImageView tripleView = new ImageView(tripleImage);
        tripleView.setFitWidth(60);
        tripleView.setFitHeight(60);
        tripleView.setLayoutX(1400);
        tripleView.setLayoutY(400);
        tripleView.setUserData("TripleShotTower");

        // Kule fiyat etiketleri
        Label singlePrice = new Label("Single Shot Tower-\n$50");
        singlePrice.setLayoutX(1470);
        singlePrice.setLayoutY(200);
        Label triplePrice = new Label("Triple Shot Tower-\n$150");
        triplePrice.setLayoutX(1470);
        triplePrice.setLayoutY(400);
        Label laserPrice = new Label("Laser Tower-\n$120");
        laserPrice.setLayoutX(1470);
        laserPrice.setLayoutY(300);
        Label missilePrice = new Label("Missile Launcher Tower-\n$200");
        missilePrice.setLayoutX(1470);
        missilePrice.setLayoutY(500);

        // Kule ikonlarına sürükleme özelliği ekle
        ImageView[] towerIcons = {laserView, missileView, singleView, tripleView};
        for (ImageView view : towerIcons) {
            view.setOnDragDetected(event -> {
                Dragboard db = view.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(view.getUserData().toString());
                db.setContent(content);
                event.consume();
            });
        }

        // Tüm UI bileşenlerini ekrana ekle
        root.getChildren().addAll(towerIcons);
        root.getChildren().addAll(livesLabel, moneyLabel, nextWaveLabel);
        root.getChildren().addAll(singlePrice, triplePrice, laserPrice, missilePrice);

        // Sahneyi oluştur ve göster
        Scene scene = new Scene(root, 1600, 800);
        stage.setScene(scene);
        stage.setTitle("Level " + level);
        stage.show();
    }

    // Dalgaları başlat
    private void startWaves() {
        waveIndex = 0;
        if (!waves.isEmpty()) {
            // İlk dalga için geri sayım başlat
            startNextWaveCountdown(waves.get(waveIndex).delayBeforeStart);
            spawnWave(waves.get(waveIndex)); // İlk dalgayı başlat
        }
    }
    
    // Sonraki dalga için geri sayım başlat
    private void startNextWaveCountdown(double seconds) {
        if (nextWaveTimer != null) {
            nextWaveTimer.stop();
        }
        
        nextWaveCountdown = seconds;
        nextWaveTimer = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                nextWaveCountdown -= 1;
                updateNextWaveLabel();
                
                if (nextWaveCountdown <= 0 && nextWaveTimer != null) {
                    nextWaveTimer.stop();
                }
            })
        );
        nextWaveTimer.setCycleCount(Timeline.INDEFINITE);
        nextWaveTimer.play();
    }
    
    // Sonraki dalga etiketini güncelle
    private void updateNextWaveLabel() {
        if (waveIndex < waves.size() - 1) {
            nextWaveLabel.setText(String.format("Next Wave in: %.0fs", nextWaveCountdown));
        } else {
            nextWaveLabel.setText(String.format("Final Wave in: %.0fs", nextWaveCountdown));
        }
    }

    // Dalga oluştur
    private void spawnWave(Wave wave) {
        if (waveTimeline != null) {
            waveTimeline.stop();
        }

        waveTimeline = new Timeline();
        
        // Dalgadaki tüm düşmanlar için zaman çizelgesi oluştur
        for (int i = 0; i < wave.enemyCount; i++) {
            KeyFrame kf = new KeyFrame(
                Duration.seconds(wave.delayBeforeStart + i * wave.spawnInterval),
                e -> spawnEnemy()
            );
            waveTimeline.getKeyFrames().add(kf);
        }

        // Dalga bittiğinde sonraki dalgayı başlat
        waveTimeline.setOnFinished(e -> {
            waveIndex++;
            if (waveIndex < waves.size()) {
                startNextWaveCountdown(waves.get(waveIndex).delayBeforeStart);
                spawnWave(waves.get(waveIndex));
            } else {
                nextWaveLabel.setText("No more waves!");
            }
        });

        waveTimeline.play();
    }

    // Seviye dosyasını yükle
    private void loadLevelFromFile(String filename) {
        path.clear();
        waves.clear();
        orangeCells.clear();

        try (Scanner scanner = new Scanner(new java.io.File(filename))) {
            boolean readingWaves = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // Dosyadan seviye bilgilerini oku
                if (line.startsWith("WIDTH:")) {
                    COLS = Integer.parseInt(line.substring(6).trim());
                } else if (line.startsWith("HEIGHT:")) {
                    ROWS = Integer.parseInt(line.substring(7).trim());
                } else if (line.equalsIgnoreCase("WAVE_DATA:")) {
                    readingWaves = true;
                } else if (readingWaves) {
                    // Dalga bilgilerini oku
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        int count = Integer.parseInt(parts[0].trim());
                        double interval = Double.parseDouble(parts[1].trim());
                        double delay = Double.parseDouble(parts[2].trim());
                        waves.add(new Wave(count, interval, delay));
                    }
                } else {
                    // Yol bilgilerini oku
                    String[] coords = line.split(",");
                    if (coords.length == 2) {
                        int row = Integer.parseInt(coords[0].trim());
                        int col = Integer.parseInt(coords[1].trim());
                        path.add(new int[]{row, col});
                    }
                }
            }

            // Hücre boyutunu ayarla
            CELL_SIZE = Math.min(1600/ COLS, 800 / ROWS);

            // Tüm hücreleri turuncu olarak işaretle
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    orangeCells.add(row + "," + col);
                }
            }

        } catch (Exception e) {
            System.err.println("Level file not found " + e.getMessage());
        }
    }

    // Izgarayı çiz
    private void drawGrid(GraphicsContext gc) {
        if (path == null) return;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int r = row;
                final int c = col;
                boolean isPath = path.stream().anyMatch(p -> p[0] == r && p[1] == c);

                // Yol hücrelerini ve normal hücreleri farklı renklerde çiz
                if (isPath) {
                    gc.setFill(Color.rgb(235, 223, 224));
                } else {
                    gc.setFill(Color.ORANGE.deriveColor(1,1,1,0.5));
                }

                gc.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                gc.setStroke(Color.WHITE);
                gc.strokeRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    // Uygulama giriş noktası
    public static void main(String[] args) {
        launch(args);
    }
}