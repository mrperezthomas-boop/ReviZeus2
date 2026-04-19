package com.revizeus.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════════
 * OLYMPIAN PARTICLES VIEW
 * ═══════════════════════════════════════════════════════════════
 * Vue custom ultra légère pour dessiner des particules olympiennes :
 * - poussières dorées
 * - braises cyan
 * - étincelles électriques
 *
 * NOUVEAU v10 :
 * ✅ Méthode startPerfectExplosion() pour animation 100% quiz
 * ✅ Support explosion depuis un point central
 * ✅ Physique réaliste avec vélocité et gravité
 *
 * Objectif :
 * créer une ambiance premium sans dépendre d'assets externes.
 *
 * DA RESPECTÉE :
 * - fond sombre
 * - or / cyan / bleu électrique
 * - rendu magique, discret, propre
 *
 * NOTES TECHNIQUES :
 * - pas de clic
 * - pas de focus
 * - animation gérée en invalidate()
 * - coût faible pour rester fluide sur mobile
 * ═══════════════════════════════════════════════════════════════
 */
class OlympianParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /**
     * Profils de particules selon l'écran.
     * Cela permet d'avoir des ambiances cohérentes sans recréer une autre classe.
     */
    enum class ParticleMode {
        SAVOIR,
        TEMPLE,
        TRAINING,
        QUIZ,
        PERFECT  // NOUVEAU : Mode explosion pour 100%
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var driftX: Float,
        var alpha: Int,
        var twinkleSpeed: Float,
        var twinkleDirection: Int,
        var color: Int,
        // NOUVEAU : Pour mode PERFECT
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var gravity: Float = 0f,
        var lifetime: Long = 0L,
        var maxLifetime: Long = 0L
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mode: ParticleMode = ParticleMode.SAVOIR
    private var particleCount: Int = 34

    private var lastFrameTime = 0L
    private var isAnimating = false

    // NOUVEAU : Pour mode explosion
    private var explosionStartTime = 0L
    private var explosionDuration = 3000L // 3 secondes

    init {
        // La vue ne doit jamais gêner les interactions UI
        isClickable = false
        isFocusable = false
        alpha = 0.92f
    }

    /**
     * Permet de choisir le profil visuel selon l'écran.
     */
    fun configure(mode: ParticleMode) {
        this.mode = mode
        particleCount = when (mode) {
            ParticleMode.SAVOIR -> 28
            ParticleMode.TEMPLE -> 36
            ParticleMode.TRAINING -> 42
            ParticleMode.QUIZ -> 48
            ParticleMode.PERFECT -> 0 // Géré par startPerfectExplosion
        }
        rebuildParticles()
    }

    /**
     * Lancement explicite de l'animation.
     */
    fun start() {
        if (isAnimating) return
        isAnimating = true
        lastFrameTime = SystemClock.uptimeMillis()
        invalidate()
    }

    /**
     * Arrêt de l'animation.
     */
    fun stop() {
        isAnimating = false
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * NOUVEAU : EXPLOSION PARFAITE POUR 100%
     * ═══════════════════════════════════════════════════════════════
     * Crée une explosion de particules depuis un point central
     * avec physique réaliste (vélocité radiale + gravité)
     * 
     * @param centerX Position X du centre de l'explosion
     * @param centerY Position Y du centre de l'explosion
     * @param color Couleur principale des particules
     * @param particleCount Nombre de particules à créer (recommandé 200-300)
     * ═══════════════════════════════════════════════════════════════
     */
    fun startPerfectExplosion(
        centerX: Float,
        centerY: Float,
        color: Int,
        particleCount: Int = 250
    ) {
        mode = ParticleMode.PERFECT
        particles.clear()

        explosionStartTime = SystemClock.uptimeMillis()

        // Créer les particules d'explosion
        repeat(particleCount) {
            // Angle aléatoire pour explosion radiale
            val angle = Random.nextFloat() * 2 * PI.toFloat()

            // Vitesse radiale variable (plus rapide = va plus loin)
            val speed = Random.nextFloat() * 15f + 5f

            // Vélocités X et Y selon l'angle
            val velocityX = cos(angle) * speed
            val velocityY = sin(angle) * speed

            // Taille variable
            val radius = Random.nextFloat() * 4f + 2f

            // Gravité variable (certaines particules tombent plus vite)
            val gravity = Random.nextFloat() * 0.3f + 0.1f

            // Durée de vie variable
            val maxLifetime = Random.nextLong(1500, 3000)

            // Variation de couleur autour du doré
            val particleColor = when (Random.nextInt(0, 10)) {
                in 0..6 -> color // 70% couleur principale (or)
                in 7..8 -> Color.parseColor("#FFFFFF") // 20% blanc
                else -> Color.parseColor("#FFF4CC") // 10% or clair
            }

            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    radius = radius,
                    speedY = 0f, // Non utilisé en mode PERFECT
                    driftX = 0f, // Non utilisé en mode PERFECT
                    alpha = 255,
                    twinkleSpeed = 0f, // Non utilisé en mode PERFECT
                    twinkleDirection = 0, // Non utilisé en mode PERFECT
                    color = particleColor,
                    velocityX = velocityX,
                    velocityY = velocityY,
                    gravity = gravity,
                    lifetime = 0L,
                    maxLifetime = maxLifetime
                )
            )
        }

        start()
    }

    /**
     * Logique de mise à jour selon le mode.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isAnimating) return

        val now = SystemClock.uptimeMillis()
        val delta = now - lastFrameTime
        lastFrameTime = now

        val deltaFactor = min(delta / 16f, 2.5f)

        if (mode == ParticleMode.PERFECT) {
            updateAndDrawExplosion(canvas, deltaFactor, now)
        } else {
            updateAndDrawNormalParticles(canvas, deltaFactor)
        }

        invalidate()
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * NOUVEAU : Mise à jour et rendu de l'explosion parfaite
     * ═══════════════════════════════════════════════════════════════
     */
    private fun updateAndDrawExplosion(canvas: Canvas, deltaFactor: Float, now: Long) {
        val elapsedTime = now - explosionStartTime

        // Arrêter après la durée définie
        if (elapsedTime > explosionDuration) {
            stop()
            particles.clear()
            return
        }

        // Mettre à jour chaque particule
        particles.forEach { particle ->
            particle.lifetime += deltaFactor.toLong()

            // Appliquer la vélocité
            particle.x += particle.velocityX * deltaFactor
            particle.y += particle.velocityY * deltaFactor

            // Appliquer la gravité (fait tomber les particules)
            particle.velocityY += particle.gravity * deltaFactor

            // Fade out progressif basé sur la durée de vie
            val lifetimeRatio = particle.lifetime.toFloat() / particle.maxLifetime.toFloat()
            particle.alpha = ((1f - lifetimeRatio) * 255).toInt().coerceIn(0, 255)

            // Réduction de la taille vers la fin
            val sizeRatio = 1f - (lifetimeRatio * 0.3f)
            val currentRadius = particle.radius * sizeRatio

            // Dessiner la particule si elle est encore visible
            if (particle.alpha > 0 && particle.y < height + 50) {
                drawExplosionParticle(canvas, particle, currentRadius)
            }
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * NOUVEAU : Rendu d'une particule d'explosion
     * ═══════════════════════════════════════════════════════════════
     */
    private fun drawExplosionParticle(canvas: Canvas, particle: Particle, radius: Float) {
        // Aura externe (plus prononcée que les particules normales)
        paint.color = particle.color
        paint.alpha = min(100, particle.alpha / 3)
        canvas.drawCircle(
            particle.x,
            particle.y,
            radius * 3f,
            paint
        )

        // Noyau lumineux principal
        paint.color = particle.color
        paint.alpha = particle.alpha
        canvas.drawCircle(
            particle.x,
            particle.y,
            radius,
            paint
        )

        // Point central ultra brillant
        paint.color = Color.WHITE
        paint.alpha = (particle.alpha * 0.8f).toInt()
        canvas.drawCircle(
            particle.x,
            particle.y,
            radius * 0.4f,
            paint
        )
    }

    /**
     * Mise à jour et rendu des particules normales (modes SAVOIR, TEMPLE, etc.)
     */
    private fun updateAndDrawNormalParticles(canvas: Canvas, deltaFactor: Float) {
        particles.forEach { particle ->
            updateParticle(particle, deltaFactor)
            drawParticle(canvas, particle)
        }
    }

    /**
     * Reconstruit le tableau de particules (pour modes normaux).
     */
    private fun rebuildParticles() {
        if (mode == ParticleMode.PERFECT) return // Géré par startPerfectExplosion

        particles.clear()
        repeat(particleCount) {
            particles.add(createParticle(randomSpawnAnywhere = true))
        }
    }

    /**
     * Crée une nouvelle particule selon le mode.
     */
    private fun createParticle(randomSpawnAnywhere: Boolean): Particle {
        val spawnX: Float = Random.nextFloat() * width

        val spawnY: Float = if (randomSpawnAnywhere) {
            Random.nextFloat() * height
        } else {
            height + Random.nextFloat() * 120f
        }

        val baseRadius = when (mode) {
            ParticleMode.SAVOIR -> Random.nextFloat() * 1.8f + 1.0f
            ParticleMode.TEMPLE -> Random.nextFloat() * 2.2f + 0.9f
            ParticleMode.TRAINING -> Random.nextFloat() * 2.4f + 1.2f
            ParticleMode.QUIZ -> Random.nextFloat() * 2.8f + 1.4f
            ParticleMode.PERFECT -> 0f // Non utilisé
        }

        val baseSpeed = when (mode) {
            ParticleMode.SAVOIR -> Random.nextFloat() * 1.1f + 0.4f
            ParticleMode.TEMPLE -> Random.nextFloat() * 1.3f + 0.5f
            ParticleMode.TRAINING -> Random.nextFloat() * 1.6f + 0.6f
            ParticleMode.QUIZ -> Random.nextFloat() * 1.9f + 0.7f
            ParticleMode.PERFECT -> 0f // Non utilisé
        }

        val drift = (Random.nextFloat() - 0.5f) * 0.8f

        val alphaValue = when (mode) {
            ParticleMode.SAVOIR -> Random.nextInt(70, 155)
            ParticleMode.TEMPLE -> Random.nextInt(80, 165)
            ParticleMode.TRAINING -> Random.nextInt(85, 175)
            ParticleMode.QUIZ -> Random.nextInt(90, 185)
            ParticleMode.PERFECT -> 255 // Non utilisé
        }

        return Particle(
            x = spawnX,
            y = spawnY,
            radius = baseRadius,
            speedY = baseSpeed,
            driftX = drift,
            alpha = alphaValue,
            twinkleSpeed = Random.nextFloat() * 2.6f + 0.4f,
            twinkleDirection = if (Random.nextBoolean()) 1 else -1,
            color = pickColorForMode()
        )
    }

    /**
     * Palette cohérente avec la charte RéviZeus :
     * - or
     * - cyan
     * - bleu électrique
     * - très léger blanc marbre
     */
    private fun pickColorForMode(): Int {
        val pool = when (mode) {
            ParticleMode.SAVOIR -> listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#CFFBFF"),
                Color.parseColor("#6FE8FF")
            )

            ParticleMode.TEMPLE -> listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#FFF4CC"),
                Color.parseColor("#7FDBFF"),
                Color.parseColor("#1E90FF")
            )

            ParticleMode.TRAINING -> listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#1E90FF")
            )

            ParticleMode.QUIZ -> listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#1E90FF"),
                Color.parseColor("#FFFFFF")
            )

            ParticleMode.PERFECT -> Color.parseColor("#FFD700") // Doré par défaut
        }
        return if (pool is List<*>) (pool.random() as Int) else (pool as Int)
    }

    /**
     * Déplace la particule verticalement vers le haut avec une petite dérive.
     * Lorsqu'elle sort de l'écran, elle réapparaît en bas.
     */
    private fun updateParticle(particle: Particle, deltaFactor: Float) {
        particle.y -= particle.speedY * deltaFactor
        particle.x += particle.driftX * deltaFactor

        // Twinkle doux pour éviter un effet trop "cheap"
        particle.alpha += (particle.twinkleSpeed * particle.twinkleDirection).toInt()

        if (particle.alpha >= 200) {
            particle.alpha = 200
            particle.twinkleDirection = -1
        } else if (particle.alpha <= 45) {
            particle.alpha = 45
            particle.twinkleDirection = 1
        }

        // Bouclage horizontal discret
        if (particle.x < -20f) particle.x = width + 20f
        if (particle.x > width + 20f) particle.x = -20f

        // Si la particule sort par le haut, on la recrée en bas
        if (particle.y < -20f) {
            val recreated = createParticle(randomSpawnAnywhere = false)
            particle.x = recreated.x
            particle.y = recreated.y
            particle.radius = recreated.radius
            particle.speedY = recreated.speedY
            particle.driftX = recreated.driftX
            particle.alpha = recreated.alpha
            particle.twinkleSpeed = recreated.twinkleSpeed
            particle.twinkleDirection = recreated.twinkleDirection
            particle.color = recreated.color
        }
    }

    /**
     * Dessin simple : un cercle principal + une légère aura.
     * Cela donne un rendu plus premium qu'un simple point brut.
     */
    private fun drawParticle(canvas: Canvas, particle: Particle) {
        // Aura externe
        paint.color = particle.color
        paint.alpha = min(110, particle.alpha / 2)
        canvas.drawCircle(
            particle.x,
            particle.y,
            particle.radius * 2.2f,
            paint
        )

        // Noyau lumineux
        paint.color = particle.color
        paint.alpha = particle.alpha
        canvas.drawCircle(
            particle.x,
            particle.y,
            particle.radius,
            paint
        )
    }
}