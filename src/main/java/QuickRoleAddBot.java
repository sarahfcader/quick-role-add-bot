import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;

import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;
//////


public class QuickRoleAddBot extends ListenerAdapter {

    public static void main(String[] args) {

        try {
            // Build JDA
            Dotenv dotenv = Dotenv.configure().load();
            JDA bot = JDABuilder.createLight(dotenv.get("BOT_TOKEN"))
                    .addEventListeners(new QuickRoleAddBot())
                    .setChunkingFilter(ChunkingFilter.ALL) // enable member chunking for all guilds
                    .setMemberCachePolicy(MemberCachePolicy.ALL) // ignored if chunking enabled
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build();

            // These commands take up to an hour to be activated after creation/update/delete
            CommandListUpdateAction commands = bot.updateCommands();

            // Moderation commands with required options
            commands.addCommands(
                    Commands.slash("mass-assign", "Assign roles to users matching your specified requirements:")
                            .addOptions(new OptionData(ROLE, "role", "The role to be assigned.")
                                    .setRequired(true))
                            .addOptions(new OptionData(CHANNEL, "channel", "Channel that the user must have participated in.").setRequired(true))
                            .addOptions(new OptionData(INTEGER, "num-messages", "The number of messages to check against requirements (starting from the most recent).").setRequired(true))
                            .addOptions(new OptionData(INTEGER, "max-roles-to-assign", "The desired maximum number of roles to give out.").setRequired(true))
                            .addOptions(new OptionData(INTEGER, "min-message-length", "The min. # of characters in a message to get role.").setRequired(false))
                    //.addOptions(new OptionData(STRING, "message-contains", "Any strings the message must have contained, separate with spaces.").setRequired(false))


            );

            // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
            commands.queue();
        } catch (LoginException e) {
            System.out.println("Bot login failed.");
            e.printStackTrace();
        }
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;
        switch (event.getName()) {
            case "mass-assign":
                massAssign(event);
                break;
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
        TextChannel channel = event.getOption("channel").getAsTextChannel();
        int limit = event.getOption("num-messages").getAsInt();
        int maxAssigns = event.getOption("max-roles-to-assign").getAsInt();
        int minLength = event.getOption("min-message-length") != null ? event.getOption("min-message-length").getAsInt() : 1;

        //String requiredMessageContainsStrings = event.getOption("message-contains") != null ? event.getOption("message-contains").getAsString();

        Guild guild = event.getGuild();

        AtomicInteger rolesAssigned = new AtomicInteger();

        MessagePaginationAction action = channel.getIterableHistory();
        action.stream()
                .limit(limit)
                .filter(m -> !m.getAuthor().isBot())
                .map(m -> m.getAuthor())
                .distinct()
                .filter(user -> guild.getMember(user) != null)
                .filter(user -> !guild.getMember(user).getRoles().contains(role))
                .forEach(u -> assignRoleToMember(guild, role, u.getId(), rolesAssigned, maxAssigns));

        hook.sendMessage(rolesAssigned.get() + " roles assigned.").queue();
    }

    void assignRoleToMember(Guild guild, Role role, String id, AtomicInteger assigned, int maxAssigns) {
        if (assigned.get() > maxAssigns) {
            return;
        } else {
            try {
                guild.addRoleToMember(id, role).complete();
                assigned.getAndIncrement();
            } catch(ErrorResponseException e) {
                e.printStackTrace();
            }
        }
    }

}