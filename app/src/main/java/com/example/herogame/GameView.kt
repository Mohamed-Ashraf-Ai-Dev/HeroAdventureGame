package com.example.herogame

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private val holder: SurfaceHolder = getHolder()
    private var canvas: Canvas? = null
    private val paint: Paint = Paint()

    // Player properties
    private var playerX = 100f
    private var playerY = 0f
    private var playerSize = 100f
    private var velocityY = 0f
    private val gravity = 2.5f
    private val jumpStrength = -45f
    private var isJumping = false

    // Game State
    private var score = 0
    private var level = 1
    private var screenWidth = 0
    private var screenHeight = 0
    private var isGameOver = false

    // Obstacles and Coins
    private val obstacles = mutableListOf<RectF>()
    private val coins = mutableListOf<RectF>()
    private val starParticles = mutableListOf<PointF>()

    init {
        paint.isAntiAlias = true
        // Initialize background stars
        for (i in 0..50) {
            starParticles.add(PointF(Math.random().toFloat() * 2000, Math.random().toFloat() * 1000))
        }
    }

    private fun setupLevel() {
        obstacles.clear()
        coins.clear()
        
        // Generate a sequence of obstacles and coins for the current level
        val baseDistance = 1000f
        for (i in 1..10 + (level * 2)) {
            val ox = baseDistance * i + (Math.random() * 600).toFloat()
            val height = 150f + (Math.random() * 200).toFloat()
            obstacles.add(RectF(ox, screenHeight - 200f - height, ox + 120f, screenHeight - 200f))
            
            // Add coins above or between obstacles
            val cx = ox + 60f
            val cy = screenHeight - 500f - (Math.random() * 300).toFloat()
            coins.add(RectF(cx, cy, cx + 60f, cy + 60f))
        }
    }

    override fun run() {
        while (isPlaying) {
            if (!isGameOver) {
                update()
            }
            draw()
            control()
        }
    }

    private fun update() {
        // Apply Gravity
        velocityY += gravity
        playerY += velocityY

        // Ground collision
        val groundY = screenHeight - 200f - playerSize
        if (playerY > groundY) {
            playerY = groundY
            velocityY = 0f
            isJumping = false
        }

        // Move world (parallax effect)
        val speed = 15f + (level * 1.5f)
        
        // Update Stars
        for (star in starParticles) {
            star.x -= speed / 3
            if (star.x < 0) star.x = screenWidth.toFloat() + 100
        }

        // Update Obstacles
        val playerRect = RectF(playerX + 10, playerY + 10, playerX + playerSize - 10, playerY + playerSize - 10)
        
        val iteratorObs = obstacles.iterator()
        while (iteratorObs.hasNext()) {
            val obs = iteratorObs.next()
            obs.offset(-speed, 0f)
            
            if (RectF.intersects(playerRect, obs)) {
                isGameOver = true
            }
            
            if (obs.right < 0 && !isGameOver) {
                // Potential to remove but we keep for level completion check
            }
        }

        // Update Coins
        val iteratorCoins = coins.iterator()
        while (iteratorCoins.hasNext()) {
            val coin = iteratorCoins.next()
            coin.offset(-speed, 0f)
            
            if (RectF.intersects(playerRect, coin)) {
                score += 10
                iteratorCoins.remove()
                
                // Level Up every 100 points
                if (score > 0 && score % 100 == 0) {
                    level++
                    setupLevel()
                }
            }
            
            if (coin.right < 0) {
                iteratorCoins.remove()
            }
        }
        
        // Check if level cleared (all obstacles passed)
        if (obstacles.isNotEmpty() && obstacles.all { it.right < 0 }) {
            level++
            setupLevel()
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()
            if (canvas == null) return
            
            screenWidth = canvas!!.width
            screenHeight = canvas!!.height
            
            if (obstacles.isEmpty() && !isGameOver) setupLevel()

            // 1. Draw Sky Gradient
            val gradient = LinearGradient(0f, 0f, 0f, screenHeight.toFloat(), 
                Color.parseColor("#1A237E"), Color.parseColor("#3F51B5"), Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas!!.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.shader = null

            // 2. Draw Stars
            paint.color = Color.WHITE
            for (star in starParticles) {
                canvas!!.drawCircle(star.x, star.y, 3f, paint)
            }

            // 3. Draw Ground
            paint.color = Color.parseColor("#2E7D32")
            canvas!!.drawRect(0f, screenHeight - 200f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.color = Color.parseColor("#388E3C")
            canvas!!.drawRect(0f, screenHeight - 200f, screenWidth.toFloat(), screenHeight - 180f, paint)

            // 4. Draw Player (Hero)
            paint.color = Color.parseColor("#FFC107") // Golden Hero
            canvas!!.drawRoundRect(playerX, playerY, playerX + playerSize, playerY + playerSize, 25f, 25f, paint)
            // Eyes
            paint.color = Color.BLACK
            canvas!!.drawCircle(playerX + 70, playerY + 30, 8f, paint)
            
            // 5. Draw Obstacles
            paint.color = Color.parseColor("#D32F2F") // Red obstacles
            for (obs in obstacles) {
                canvas!!.drawRoundRect(obs, 15f, 15f, paint)
            }

            // 6. Draw Coins
            paint.color = Color.parseColor("#FFEB3B")
            for (coin in coins) {
                canvas!!.drawOval(coin, paint)
            }

            // 7. Draw UI
            paint.color = Color.WHITE
            paint.textSize = 70f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas!!.drawText("Score: $score", 50f, 100f, paint)
            canvas!!.drawText("Level: $level", 50f, 190f, paint)

            if (isGameOver) {
                paint.color = Color.argb(180, 0, 0, 0)
                canvas!!.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
                
                paint.color = Color.WHITE
                paint.textSize = 120f
                val text = "GAME OVER!"
                val textWidth = paint.measureText(text)
                canvas!!.drawText(text, (screenWidth - textWidth) / 2, screenHeight / 2f, paint)
                
                paint.textSize = 60f
                val subText = "Tap to Restart"
                val subWidth = paint.measureText(subText)
                canvas!!.drawText(subText, (screenWidth - subWidth) / 2, screenHeight / 2f + 150, paint)
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try {
            Thread.sleep(16) // ~60 FPS
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }

    fun pause() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                resetGame()
            } else if (!isJumping) {
                velocityY = jumpStrength
                isJumping = true
            }
        }
        return true
    }

    private fun resetGame() {
        score = 0
        level = 1
        playerY = 0f
        velocityY = 0f
        isGameOver = false
        setupLevel()
    }
}
