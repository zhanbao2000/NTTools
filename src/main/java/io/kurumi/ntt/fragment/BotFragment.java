package io.kurumi.ntt.fragment;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.model.Callback;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.model.Query;
import io.kurumi.ntt.utils.BotLog;
import java.util.LinkedList;
import java.util.List;
import io.kurumi.ntt.model.request.Send;
import com.pengrad.telegrambot.response.GetMeResponse;
import okhttp3.OkHttpClient;

public abstract class BotFragment extends Fragment implements UpdatesListener {

	public User me;

    private TelegramBot bot;
    private LinkedList<Fragment> fragments = new LinkedList<>();
    private String token;

    {

        fragments.add(this);

    }

    public BotFragment() {
        origin = this;
    }

    @Override
    public TelegramBot bot() {

        return bot;

    }

    public void addFragment(Fragment fragment) {

        fragment.origin = this;
        fragments.add(fragment);


    }

	public void remFragment(Fragment fragment) {

        fragments.remove(fragment);


    }


    public abstract String botName();

    /*

     public boolean isLongPulling() {

     return false;

     }

     */

    @Override
    public int process(List<Update> updates) {

		for (final Update update : updates) {

			try {

				process(update);

			} catch (Exception e) {

				BotLog.error("更新出错",e);

				Launcher.INSTANCE.uncaughtException(Thread.currentThread(),e);

			}


		}

		return CONFIRMED_UPDATES_ALL;

    }

    @Override
    public boolean onMsg(UserData user,Msg msg) {

        if ("cancel".equals(msg.command())) {

            msg.send("你要取消什么？ >_<").exec();

            return true;

        }

        return false;

    }

    @Override
    public boolean onPPM(UserData user,Msg msg) {

        if ("cancel".equals(msg.command())) {

            user.point = null;

            user.savePoint();

            msg.send("取消成功 ~").exec();

            return true;
            
        }

        return false;
        
    }



    public void process(final Update update) {

        if (update.message() != null) {

            UserData user = UserData.get(update.message().from());

            BotLog.process(user,update);

            if (update.message().chat().type() == Chat.Type.Private && user != null && user.point != null) {

                for (Fragment fragmnet : fragments) {

                    if (fragmnet.onPPM(user,new Msg(fragmnet,update.message()))) {

                        return;

                    }

                }

                return;

            }
            
            for (Fragment fragmnet : fragments) {

                if (fragmnet.onMsg(user,new Msg(fragmnet,update.message()))) {

                    return;

                }

            }

            switch (update.message().chat().type()) {

                case Private: {

                        for (Fragment fragmnet : fragments) {

                            if (fragmnet.onNPM(user,new Msg(fragmnet,update.message()))) {
                                
                                return;
 
                            }

                        }


                        break;

                    }

                case group: {

                        for (Fragment fragmnet : fragments) {

                            if (fragmnet.onGroupMsg(user,new Msg(fragmnet,update.message()),false)) {

                                return;

                            }

                        }

                        break;

                    }

                default: {

                        for (Fragment fragmnet : fragments) {

                            if (fragmnet.onGroupMsg(user,new Msg(fragmnet,update.message()),true)) {

                                return;

                            }

                        }

                    }

            }

        } else if (update.channelPost() != null) {

            UserData user = update.channelPost().from() != null ? UserData.get(update.channelPost().from()) : null;

            for (Fragment fragmnet : fragments) {

                if (fragmnet.onChanPost(user,new Msg(fragmnet,update.channelPost()))) {

                    return;

                }

            }

        } else if (update.callbackQuery() != null) {

            UserData user = UserData.get(update.callbackQuery().from());

            for (Fragment fragmnet : fragments) {

                if (fragmnet.onCallback(user,new Callback(fragmnet,update.callbackQuery()))) {

                    return;

                }

            }

        } else if (update.inlineQuery() != null) {

            UserData user = UserData.get(update.channelPost().from());

            for (Fragment fragmnet : fragments) {

                if (fragmnet.onQuery(user,new Query(fragmnet,update.inlineQuery()))) {

                    return;

                }

            }

        }

    }
    
    public boolean silentStart() {

        token = Env.get("token." + botName());

        if (token == null || !Env.verifyToken(token)) {

            return false;

        }

        bot = new TelegramBot.Builder(token).build();

        GetMeResponse resp = bot.execute(new GetMe());

        if (resp == null || !resp.isOk()) return false;

        me = resp.user();

        bot.execute(new DeleteWebhook());

        /*

         if (isLongPulling()) {

         */

        bot.setUpdatesListener(this,new GetUpdates());
        
        return true;
        
    }
 
    public void start() {
        
        token = Env.get("token." + botName());

        if (token == null || !Env.verifyToken(token)) {

            token = Env.inputToken(botName());

        }
        
        OkHttpClient.Builder okhttpClient = new OkHttpClient.Builder();

        okhttpClient.networkInterceptors().clear();
        
        bot = new TelegramBot.Builder(token)
        .okHttpClient(okhttpClient.build()).build();

		me = bot.execute(new GetMe()).user();

		bot.execute(new DeleteWebhook());

        /*

         if (isLongPulling()) {

         */

        bot.setUpdatesListener(this,new GetUpdates());

        /*

         } else {

         bot.execute(new SetWebhook().url("https://" + BotConf.SERVER_DOMAIN + "/" + token));

         TGWebHookF.bots.put(token, this);

         }

         */

    }

    public void stop() {
        
        bot().removeGetUpdatesListener();

        BotLog.info("BOT已停止 :)");
        
    }


}