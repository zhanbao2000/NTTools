package io.kurumi.ntt;

import cn.hutool.core.lang.*;
import com.mongodb.*;
import com.pengrad.telegrambot.request.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.fragment.*;
import io.kurumi.ntt.funcs.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.twitter.stream.*;
import io.kurumi.ntt.twitter.track.*;
import io.kurumi.ntt.utils.*;
import java.util.*;

public class Launcher extends BotFragment implements Thread.UncaughtExceptionHandler {

    public static final Launcher INSTANCE = new Launcher();

    public Launcher() {

        addFragment(Ping.INSTANCE);

        addFragment(StickerManage.INSTANCE);

        addFragment(TwitterDelete.INSTANCE);

        addFragment(Backup.INSTANCE);

        addFragment(GroupRepeat.INSTANCE);

		addFragment(TwitterUI.INSTANCE);

        addFragment(TwitterArchive.INSTANCE);

        addFragment(FollowersTrack.INSTANCE);

        addFragment(UserTrack.INSTANCE);

        addFragment(StatusUI.INSTANCE);

		addFragment(ChineseAction.INSTANCE);

        addFragment(GroupProtecter.INSTANCE);

        addFragment(YourGroupRule.INSTANCE);

        // addFragment(AnalysisJsp.INSTANCE);

        addFragment(HideMe.INSTANCE);

        // addFragment(MusicSearch.INSTANCE);

        // addFragment(AntiTooManyMsg.INSTANCE);

        addFragment(Maven.INSTANCE);

		addFragment(Quit.INSTANCE);

    }

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);

		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));

        /*

		 BotServer.INSTACNCE.fragments.add(TGWebHookF.INSTANCE);

		 try {

		 BotServer.INSTACNCE.start();

		 } catch (IOException e) {

		 BotLog.error("端口被占用无法启动", e);

		 return;

		 }

		 */

        String dbAddr = Env.getOrDefault("db_address","127.0.0.1");
        Integer dbPort = Integer.parseInt(Env.getOrDefault("db_port","27017"));

        while (!initDB(dbAddr,dbPort)) {

            System.out.print("输入MongoDb地址 : ");
            dbAddr = Console.scanner().nextLine();

            try {

                System.out.print("输入MongoDb端口 : ");
                dbPort = Console.scanner().nextInt();

                Env.set("db_address",dbAddr);
                Env.set("db_port",dbPort);

            } catch (Exception e) {}

        }

        INSTANCE.start();


    }

    static boolean initDB(String dbAddr,Integer dbPort) {

        try {

            BotDB.init(dbAddr,dbPort);

            return true;

        } catch (MongoException e) {

            return false;

        }

    }

    @Override
    public String botName() {

        return "NTTBot";

    }

    @Override
    public boolean onMsg(UserData user,Msg msg) {

        if (super.onMsg(user,msg)) return true;

        if ("start".equals(msg.command()) && msg.params().length == 0) {

			if (msg.isPrivate()) {

				msg.send(" ヾ(･ω･｀＝´･ω･)ﾉ♪ ").exec();
				msg.send("不加个裙玩吗 ~ " + Html.a("-- 戳这里！！！ --","https://t.me/joinchat/H5gBQ1N2Mx4RuhIkq-EajQ")).html().exec();
				msg.send("输入 / 就有命令补全啦 ~ 使用 /help 查看帮助 ~").exec();
				msg.send("开源地址在 " + Html.a("NTTools","https://github.com/HiedaNaKan/NTTools") + " 欢迎打心 (๑´ڡ`๑)").html().exec();

				msg.send(
					"现在功能已经稳定 并正在重构 (≧σ≦) 可以",Html.a("联系咱",Env.DEVELOPER_URL) + " 提建议哦 ~ ",
					"欢迎新功能和想法 ヽ(○´3`)"
				).html().exec();

			} else {

				msg.send("start failed successfully ᕙ(`▿´)ᕗ").publicFailed();

			}

            return true;

        } else if ("help".equals(msg.command())) {

			if (msg.isPrivate()) {

				msg.send("这是一个不知道干什么用的bot (≧σ≦)").exec();
				msg.send(
					"/login 认证Twitter账号以使用功能 ~",
					"/logout 登出Twitter账号 bot将不保留认证信息",
					"/tstart 账号跟踪 提示当新关注者、失去关注者 、 被关注者屏蔽、关注中和关注者的账号更改 (内容见/sub)",
					"注意 : 被屏蔽再解除的关注者 (删关注) 会显示 失去关注者",
					"以及账号跟踪每十五分钟一次 (因为Twitter开放接口调用限制。所以当被回关的新关注者可能显示为 (乃关注的)",
					"/tstop 取消跟踪 以上",
					"/sub <推油链接|用户名|用户ID> 跟踪用户账号更改 (ID,名称,头像等 以及停用、冻结和回档",
					"/unsub 取消跟踪 以上",
					"/sublist 查看跟踪中列表 以上",
					"/unsuball 取消所有跟踪 以上",
					"/sstart 接收以上跟踪中的推文并自动存档 (开启有五分钟以内的延时)",
					"/sstop 取消接收推文流 立即生效",
					"/hide 对BOT其他用户隐藏账号更改 (内容见上/sub)",
					"/unhide 取消隐藏 以上",
					"/status <推文链接|ID> 推文存档/查看"
				).exec();

			} else {

				msg.send("这么长还是私聊看吧 ~ (。・`ω´・)").publicFailed();

			}

		}

        return false;

    }



    @Override
    public void uncaughtException(Thread thread,Throwable throwable) {

        BotLog.error("无法处理的错误,正在停止BOT",throwable);

        INSTANCE.bot().removeGetUpdatesListener();

        FTTask.stop();
        UTTask.stop();
        SubTask.stopAll();
        GroupProtecter.stop();
        Backup.AutoBackupTask.INSTANCE.stop();

		//  BotServer.INSTACNCE.stop();

        System.exit(1);

    }

    @Override
    public boolean silentStart() {

        boolean result =  super.silentStart();

        if (result) {

            new InitTask().start();

        }

        return result;

    }

    @Override
    public void start() {

        super.start();

        new InitTask().start();

    }

    @Override
    public void stop() {

        FTTask.stop();
        UTTask.stop();

        super.stop();

    }



}
