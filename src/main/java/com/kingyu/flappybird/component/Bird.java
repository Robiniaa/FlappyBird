package com.kingyu.flappybird.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.kingyu.flappybird.app.Game;
import com.kingyu.flappybird.util.Constant;
import com.kingyu.flappybird.util.GameUtil;
import com.kingyu.flappybird.util.MusicUtil;

/**fork后的注释：
*会在注释后标注Robiniaa，其余为原作者注释
*/

/**
 * 小鸟类，实现小鸟的绘制与飞行逻辑
 *
 * @author Kingyu
 */
public class Bird {
    public static final int IMG_COUNT = 8; // 图片数量
    public static final int STATE_COUNT = 4; // 状态数
    private final BufferedImage[][] birdImages; // 小鸟的图片数组对象
    private final int x;
    private int y; // 小鸟的坐标
    private int wingState; // 翅膀状态

    // 图片资源
    private BufferedImage image; // 实时的小鸟图片

    // 小鸟的状态
    private int state;  //五种状态 @Robiniaa
    public static final int BIRD_NORMAL = 0;   //final变量一旦被初始化后，就无法更改 @Robiniaa
    public static final int BIRD_UP = 1;      
    public static final int BIRD_FALL = 2;
    public static final int BIRD_DEAD_FALL = 3;
    public static final int BIRD_DEAD = 4;

    private final Rectangle birdCollisionRect; // 碰撞矩形
    public static final int RECT_DESCALE = 2; // 补偿碰撞矩形宽高的参数

    private final ScoreCounter counter; // 计分器
    private final GameOverAnimation gameOverAnimation;

    public static int BIRD_WIDTH;
    public static int BIRD_HEIGHT;

    // 在构造器中对资源初始化
    //构造方法的名字必须和所在类的名字一致，没有返回值，但不能声明void，@Robiniaa
    //访问权限可以为任意，但是一般情况下使用public方法权限，构造方法中的参数可以根据需要自行定义，参数的不同的构造方法构成重载 @Robiniaa
    //构造器格式： [修饰符，比如public] 类名 (参数列表，可以没有参数){ //这里不能有return}  @Robiniaa
    public Bird() {
        counter = ScoreCounter.getInstance(); // 计分器
        gameOverAnimation = new GameOverAnimation();

        // 读取小鸟图片资源
        birdImages = new BufferedImage[STATE_COUNT][IMG_COUNT]; //都已在前面定义：状态数为4，图片数为8 @Robiniaa
        for (int j = 0; j < STATE_COUNT; j++) {
            for (int i = 0; i < IMG_COUNT; i++) {
                birdImages[j][i] = GameUtil.loadBufferedImage(Constant.BIRDS_IMG_PATH[j][i]);
            }
        }

        assert birdImages[0][0] != null;  //Java在1.4中新增了一个关键字：assert。在程序开发过程中使用它创建一个断言(assertion)。其中的一种形式：assert condition;   @Robinaa
    //这里condition是一个必须为真(true)的表达式。如果表达式的结果为true，那么断言为真，并且无任何行动。如果表达式为false，则断言失败，则会抛出一个AssertionError对象。这个AssertionError继承于Error对象，  @Robinaa
    //而Error继承于Throwable，Error是和Exception并列的一个错误对象，通常用于表达系统级运行错误。  @Robinaa
    
        BIRD_WIDTH = birdImages[0][0].getWidth();
        BIRD_HEIGHT = birdImages[0][0].getHeight();

        // 初始化小鸟的坐标
        x = Constant.FRAME_WIDTH >> 2;  //FRAME_WIDTH和FRAME_HEIGHT表示窗口尺寸（已在Constant中定义为固定值420和640） >>表示右移  @Robiniaa
        y = Constant.FRAME_HEIGHT >> 1;

        // 初始化碰撞矩形
        int rectX = x - BIRD_WIDTH / 2;
        int rectY = y - BIRD_HEIGHT / 2;
        birdCollisionRect = new Rectangle(rectX + RECT_DESCALE, rectY + RECT_DESCALE * 2, BIRD_WIDTH - RECT_DESCALE * 3,
                BIRD_WIDTH - RECT_DESCALE * 4); // 碰撞矩形的坐标与小鸟相同
    }

    // 绘制方法
    public void draw(Graphics g) {
        movement();
        int state_index = Math.min(state, BIRD_DEAD_FALL); // 图片资源索引
        // 小鸟中心点计算
        int halfImgWidth = birdImages[state_index][0].getWidth() >> 1;
        int halfImgHeight = birdImages[state_index][0].getHeight() >> 1;
        if (velocity > 0)
            image = birdImages[BIRD_UP][0];
        g.drawImage(image, x - halfImgWidth, y - halfImgHeight, null); // x坐标于窗口1/4处，y坐标位窗口中心

        if (state == BIRD_DEAD)  //小鸟死亡 @Robiniaa
            gameOverAnimation.draw(g, this);
        else if (state != BIRD_DEAD_FALL) //此时小鸟还没有死亡 @Robiniaa
            drawScore(g);
        // 绘制碰撞矩形
//      g.setColor(Color.black);
//      g.drawRect((int) birdRect.getX(), (int) birdRect.getY(), (int) birdRect.getWidth(), (int) birdRect.getHeight());
    }

    public static final int ACC_FLAP = 14; // players speed on flapping
    public static final double ACC_Y = 2; // players downward acceleration
    public static final int MAX_VEL_Y = 15; // max vel along Y, max descend speed
    private int velocity = 0; // bird's velocity along Y, default same as playerFlapped
    private final int BOTTOM_BOUNDARY = Constant.FRAME_HEIGHT - GameBackground.GROUND_HEIGHT - (BIRD_HEIGHT / 2);

    // 小鸟的飞行逻辑
    private void movement() {
        // 翅膀状态，实现小鸟振翅飞行
        wingState++;
        image = birdImages[Math.min(state, BIRD_DEAD_FALL)][wingState / 10 % IMG_COUNT];
        if (state == BIRD_FALL || state == BIRD_DEAD_FALL) {
            freeFall();
            if (birdCollisionRect.y > BOTTOM_BOUNDARY) {
                if (state == BIRD_FALL) {
                    MusicUtil.playCrash();
                }
                die();
            }
        }
    }

    private void freeFall() {
        if (velocity < MAX_VEL_Y)
            velocity -= ACC_Y;
        y = Math.min((y - velocity), BOTTOM_BOUNDARY);
        birdCollisionRect.y = birdCollisionRect.y - velocity;
    }

    private void die() {
        counter.saveScore();
        state = BIRD_DEAD;
        Game.setGameState(Game.STATE_OVER);
    }

    // 小鸟振翅
    public void birdFlap() {
        if (keyIsReleased()) {
            if (isDead())
                return;
            MusicUtil.playFly(); // 播放音效
            state = BIRD_UP;
            if (birdCollisionRect.y > Constant.TOP_BAR_HEIGHT) {
                velocity = ACC_FLAP; // 每次振翅将速度改为上升速度
                wingState = 0; // 重置翅膀状态
            }
            keyPressed();
        }
    }

    // 小鸟下降
    public void birdFall() {
        if (isDead())
            return;
        state = BIRD_FALL;
    }

    // 小鸟坠落（已死）
    public void deadBirdFall() {
        state = BIRD_DEAD_FALL;
        MusicUtil.playCrash(); // 播放音效
        velocity = 0;  // 速度置0，防止小鸟继续上升与水管重叠
    }

    // 判断小鸟是否死亡
    public boolean isDead() {
        return state == BIRD_DEAD_FALL || state == BIRD_DEAD;
    }

    // 绘制实时分数
    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.setFont(Constant.CURRENT_SCORE_FONT);
        String str = Long.toString(counter.getCurrentScore());
        int x = Constant.FRAME_WIDTH - GameUtil.getStringWidth(Constant.CURRENT_SCORE_FONT, str) >> 1;
        g.drawString(str, x, Constant.FRAME_HEIGHT / 10);
    }

    // 重置小鸟
    public void reset() {
        state = BIRD_NORMAL; // 小鸟状态
        y = Constant.FRAME_HEIGHT >> 1; // 小鸟坐标
        velocity = 0; // 小鸟速度

        int ImgHeight = birdImages[state][0].getHeight();
        birdCollisionRect.y = y - ImgHeight / 2 + RECT_DESCALE * 2; // 小鸟碰撞矩形坐标

        counter.reset(); // 重置计分器
    }

    private boolean keyFlag = true; // 按键状态，true为已释放，使当按住按键时不会重复调用方法

    public void keyPressed() {
        keyFlag = false;
    }

    public void keyReleased() {
        keyFlag = true;
    }

    public boolean keyIsReleased() {
        return keyFlag;
    }

    public long getCurrentScore() {
        return counter.getCurrentScore();
    }

    public long getBestScore() {
        return counter.getBestScore();
    }

    public int getBirdX() {
        return x;
    }

    // 获取小鸟的碰撞矩形
    public Rectangle getBirdCollisionRect() {
        return birdCollisionRect;
    }
}
