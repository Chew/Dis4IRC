/*
 * This file is part of Dis4IRC.
 *
 * Dis4IRC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dis4IRC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Dis4IRC.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.zachbr.dis4irc.bridge.pier.discord

import io.zachbr.dis4irc.api.Message
import io.zachbr.dis4irc.bridge.Bridge
import io.zachbr.dis4irc.bridge.BridgeConfiguration
import io.zachbr.dis4irc.bridge.pier.Pier
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

class DiscordPier(private val bridge: Bridge) : Pier {
    internal val logger: Logger = bridge.logger
    private var discordApi: JDA? = null

    override fun init(config: BridgeConfiguration) {
        logger.info("Connecting to Discord API...")

        discordApi = JDABuilder()
            .setToken(config.discordApiKey)
            .setGame(Game.of(Game.GameType.DEFAULT, "IRC"))
            .addEventListener(DiscordListener(this))
            .build()
            .awaitReady()

        logger.info("Discord Bot Invite URL: ${discordApi?.asBot()?.getInviteUrl()}")
        logger.info("Connected to Discord!")
    }

    override fun shutdown() {
        discordApi?.shutdownNow()
    }

    override fun sendMessage(targetChan: String, msg: Message) {
        val discordChannel = discordApi?.getTextChannelById(targetChan)
        if (discordChannel == null) {
            logger.warn("Bridge is not present in Discord channel $targetChan!")
            return
        }

        if (!discordChannel.canTalk()) {
            logger.warn("Bridge cannot speak in ${discordChannel.name} to send message: $msg")
            return
        }

        discordChannel.sendMessage("<${msg.sender.displayName}> ${msg.contents}").complete()

        logger.debug("Took approximately ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - msg.timestamp)}ms to handle message")
    }

    /**
     * Gets the Discord bot's snowflake id
     */
    fun getBotId(): Long? {
        return discordApi?.selfUser?.idLong
    }

    /**
     * Sends a message to the bridge for processing
     */
    fun sendToBridge(message: io.zachbr.dis4irc.api.Message) {
        bridge.handleMessage(message)
    }
}