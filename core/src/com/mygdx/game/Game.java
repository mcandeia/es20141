package com.mygdx.game;

import java.util.Iterator;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import control.Assets;
import control.DropObject;
import control.Gingerman;
import control.Jellybean;
import control.OverlapTester;
import control.RainDrop;
import control.RainDropLarge;
import control.SugarDrop;

/**
 * Classe principal do Jogo
 */
public class Game extends ApplicationAdapter {

	public static final int WIDTH = 800;
	public static final int HEIGHT = 480;

	private boolean clicked;

	public enum State {
		Running, Paused, GameOver
	}

	private Vector3 touchPoint;
	private Rectangle restartBounds;

	private final int TIME_TO_SPAWN = 1000000;
	private long totalTime = 0L;
	private boolean gameOver;
	private State state = State.Running;
	private ShapeRenderer shape;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private Array<DropObject> drops;
	private long lastDropTime;
	private BitmapFont font;
	private TextureRegion background;
	private Gingerman gingerMan;

	private long initMilis;
	private Music crankDance;

	@Override
	public void create() {
		restartBounds = new Rectangle(10, 240, 300, 40);
		touchPoint = new Vector3();
		Gdx.app.log(Gdx.graphics.getWidth() + "", "HEIGHT");
		font = new BitmapFont();
		font.scale(2.5f);
		font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		drops = new Array<DropObject>();
		loadCamera();
		loadSoundAndMusics();
		loadElements();
		spawnDrop();
		Texture texture = new Texture(Gdx.files.internal("bg.jpg"));
		background = new TextureRegion(texture, 0, 0, WIDTH, HEIGHT);
		initMilis = TimeUtils.millis();
	}

	/**
	 * Carrega o personagem principal {@code gingerMan}.
	 */
	private void loadElements() {
		gingerMan = new Gingerman();

	}

	/**
	 * Carrega a m�sica do jogo {@code crankDance}.
	 */
	private void loadSoundAndMusics() {
		crankDance = Gdx.audio.newMusic(Gdx.files.internal(Assets.GAME_MUSIC));
		crankDance.setLooping(true);
		crankDance.play();
	}

	/**
	 * Carrega a {@code camera}.
	 */
	private void loadCamera() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, WIDTH, HEIGHT);
	}

	@Override
	public void render() {
		switch (state) {
		case Running:
			updateAll();
			break;
		case GameOver:
			gameOver();
			gameOver = true;
		case Paused:
			break;
		default:
			break;
		}
		restartButton();
		renderPlayer();
		renderDrops();
	}

	private void restartButton() {
		batch.begin();
		font.draw(batch, "Restart", 10, 300);
		batch.end();
		if (Gdx.input.justTouched()) {
			camera.unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY(),
					0));

			if (OverlapTester.pointInRectangle(restartBounds, touchPoint.x,
					touchPoint.y)) {
				loadCamera();
				loadElements();
				drops.clear();
				initMilis = TimeUtils.millis();
				state = State.Running;
				gameOver = false;
				return;
			}
		}
	}

	private void gameOver() {
		if (!gameOver) {
			totalTime = (TimeUtils.millis() - initMilis) / 1000;
		}
		batch.begin();
		batch.draw(background, 0, 0);
		font.draw(batch, ("Game Over ! Your Time: " + totalTime + " s"), 25,
				400);
		batch.end();
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		shape.setProjectionMatrix(camera.combined);
	}

	private void updateAll() {
		batch.begin();
		batch.draw(background, 0, 0);
		font.draw(batch, "Crashes :  " + gingerMan.getLife(), 25, 400);
		batch.end();
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		shape.setProjectionMatrix(camera.combined);
	}

	/**
	 * Renderiza todas as gotas e gr�os de a�ucar criados.
	 */
	private void renderDrops() {
		createDrops();
		batch.begin();
		for (DropObject rec : drops) {
			batch.draw(rec.getDroppable().getDropImage(), rec.x, rec.y);
		}
		batch.end();
	}

	/**
	 * Cria o cen�rio de gotas de chuva e gr�os de a�ucar.
	 */
	private void createDrops() {
		if (TimeUtils.nanoTime() - lastDropTime > TIME_TO_SPAWN) {
			spawnDrop();
		}
		Iterator<DropObject> it = drops.iterator();
		while (it.hasNext()) {
			DropObject drop = it.next();
			drop.y -= 200 * Gdx.graphics.getDeltaTime();
			if (drop.y + drop.height < 0) {
				it.remove();
			}
			if (drop.overlaps(gingerMan)) {
				drop.playSound();
				gingerMan.update();
				gingerMan.updateLife(drop.getDroppable().getModifyOfLife());
				if (!gingerMan.isAlive()) {
					this.state = State.GameOver;
				}
				it.remove();
			}

		}
	}

	/**
	 * Renderiza o objeto escolhido em {@link Game#initializeDrop}.
	 */
	private void spawnDrop() {
		DropObject drop = createDrop();
		spawnDrop(drop);
	}

	private void spawnDrop(DropObject drop) {
		Texture dropImage = drop.getDroppable().getDropImage();
		float imageWidth = dropImage.getWidth();
		drop.x = MathUtils.random(imageWidth, WIDTH - imageWidth);
		drop.y = HEIGHT;
		drops.add(drop);
		lastDropTime = TimeUtils.nanoTime();
	}

	/**
	 * Inicializa qual tipo de objeto vai cair.
	 */
	private DropObject createDrop() {
		DropObject drop;
		if (getRandPercent(5)) {
			drop = new DropObject(SugarDrop.getInstance());
		} else if (getRandPercent(1)) {
			if (getRandPercent(5)) {
				spawnDrop(new DropObject(Jellybean.getInstance()));
			}
			drop = new DropObject(RainDropLarge.getInstance());
		} else {
			drop = new DropObject(RainDrop.getInstance());
		}
		return drop;
	}

	/**
	 * Retorna se um certo evento deve ocorrer de acordo com a {@code percent}.
	 */
	public boolean getRandPercent(int percent) {
		Random rand = new Random();
		return rand.nextInt(100) <= percent;
	}

	/**
	 * Desenha o {@code gingerman}.
	 */
	private void renderPlayer() {
		if (Gdx.input.isTouched()) {
			clicked = true;
		}
		if (clicked) {
			moveGingerManToTouchLocal();
		} else {
			moveGingermanToAccelerometer();
		}
		if (gingerMan.x > WIDTH - gingerMan.width) {
			gingerMan.x = WIDTH - gingerMan.width;
		}
		if (gingerMan.x < 0) {
			gingerMan.x = 0;
		}

		renderLife();

		batch.begin();
		batch.draw(gingerMan.getGingermanImage(), gingerMan.x, gingerMan.y);
		batch.end();

	}

	private void renderLife() {
		shape.begin(ShapeType.Filled);
		Rectangle visualRectangle = gingerMan.getVisualLifeRectangle();
		shape.setColor(gingerMan.getColor());
		shape.rect(visualRectangle.x, visualRectangle.y, visualRectangle.width,
				visualRectangle.height);
		shape.end();
	}

	/**
	 * Move o {@code gingerman} de acordo com o acelerometro.
	 */
	private void moveGingermanToAccelerometer() {
		// int constant = Gdx.graphics.getHeight() / 10;
		// Vector3 vector = new Vector3(constant
		// * (Gdx.input.getAccelerometerY() + 10),
		// Gdx.input.getAccelerometerX(), 0);
		// camera.unproject(vector);
		// if (gingerMan.x < vector.x) {
		// gingerMan.x += 200 * Gdx.graphics.getDeltaTime();
		// if (gingerMan.x > vector.x) {
		// gingerMan.x = vector.x;
		// }
		// } else if (gingerMan.x > vector.x) {
		// gingerMan.x -= 200 * Gdx.graphics.getDeltaTime();
		// if (gingerMan.x < vector.x) {
		// gingerMan.x = vector.x;
		// }
		// } else {
		// clicked = false;
		// }

	}

	/**
	 * Move o {@code gingerman} para o local tocado.
	 */
	private void moveGingerManToTouchLocal() {
		Vector3 vector = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(vector);
		if (gingerMan.x < vector.x) {
			gingerMan.x += 200 * Gdx.graphics.getDeltaTime();
			if (gingerMan.x > vector.x) {
				gingerMan.x = vector.x;
			}
		} else if (gingerMan.x > vector.x) {
			gingerMan.x -= 200 * Gdx.graphics.getDeltaTime();
			if (gingerMan.x < vector.x) {
				gingerMan.x = vector.x;
			}
		} else {
			clicked = false;
		}

	}

	@Override
	public void dispose() {
		crankDance.dispose();
		batch.dispose();
		gingerMan.getGingermanImage().dispose();
		SugarDrop.getInstance().dispose();
		RainDrop.getInstance().dispose();
		RainDropLarge.getInstance().dispose();
		Jellybean.getInstance().dispose();
		background.getTexture().dispose();
	}
}
