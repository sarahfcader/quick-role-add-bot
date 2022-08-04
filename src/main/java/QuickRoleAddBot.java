import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import javax.security.auth.login.LoginException;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;
//////


public class QuickRoleAddBot extends ListenerAdapter {

    public static void main(String[] args) {

        // Build MongoClient
        try {
            // Build JDA
            JDA bot = JDABuilder.createLight("TOKEN")
                    .addEventListeners(new QuickRoleAddBot())
                    .build();

            // These commands take up to an hour to be activated after creation/update/delete
            CommandListUpdateAction commands = bot.updateCommands();

            // Moderation commands with required options
            commands.addCommands(
                    Commands.slash("massAssign", "Assign roles to users matching your specified requirements:")
                            .addOptions(new OptionData(ROLE, "role", "The role to be assigned.")
                                    .setRequired(true))
                            .addOptions(new OptionData(CHANNEL, "channel", "Channel that the user must have participated in.").setRequired(true))
                            .addOptions(new OptionData(STRING, "messageContains", "Any string the message must have contained.").setRequired(false))
                            .addOptions(new OptionData(INTEGER, "messageLength", "Min. length of the message.").setRequired(false))

            );

            // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
            commands.queue();
        } catch (LoginException e) {
            System.out.println("Bot login failed.");
        }
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;
        switch (event.getName()) {
            case "massAssign":
                massAssign(event);
            default:
                event.reply("I can't handle that command right now.").setEphemeral(true).queue();
        }
    }

    public void massAssign(SlashCommandInteractionEvent event) {
        event.deferReply().queue(); // tell the user we are thinking while handling
        InteractionHook hook = event.getHook();
        hook.setEphemeral(false);

        // SUBMISSION INFO
        Role role = event.getOption("role").getAsRole();
        GuildChannel channel = event.getOption("channel").getAsGuildChannel();
        String requiredMessageContainsStrings = event.getOption("messageContains").getAsString();

        //42 for addresses
        int requiredMessageLength = event.getOption("messageLength").getAsInt();


    }
}