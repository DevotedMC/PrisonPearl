PrisonPearl
===========

Minecraft plugin for civcraft which allows players to imprison other players inside ender pearls

Commands: 
```
   pplocate:
      description: Locates your prison pearl
      usage: /<command>
      aliases: ppl
  
   ppfeed:
      description: Feeds prison pearls
      usage: /<command>
      
   pplocateany:
      description: Locates any prison pearl in the world
      usage: /<command> player
      permission: prisonpearl.locateany
   
   ppfree:
      description: Frees a prison pearl
      usage: /<command> [player]
      aliases: ppf
      
   ppfreeany:
      description: Frees any prison pearl in the world
      usage: /<command> player
      permission: prisonpearl.freeany
      
   ppsummon:
      description: Summons a player from their prison pearl, requiring them to stay within range of their pearl
      usage: /ppsummon [player] [range]
      aliases: pps
      
   ppreturn:
      description: Returns a summoned player back to their prison pearl
      usage: /ppbanish [player]
      aliases: ppr
      
   ppkill:
      description: Instantly kills a summoned player, sending them back to their prison pearl
      usage: /ppkill [player]
      aliases: ppk
      
   ppsave:
      description: Saves all prisonpearl related data to disk
      usage: /ppsave
      permission: prisonpearl.save
      
   ppimprisonany:
      description: Imprisons any player in the world
      usage: /ppimprisonany player
      permission: prisonpearl.imprisonany
      
   ppbroadcast:
      description: Broadcast your pplocate commands to another player
      usage: /ppbroadcast player
      alias: ppb
      
   ppconfirm:
      description: Confirm reception of pplocate commands from a player
      usage: /ppconfirm [player]
      
   ppsilence:
      description: Silence reception of pplocate commands from player
      usage: /ppsilence player
     
   ppinfo:
      description: Get information about player in a pearl
      usage: /ppinfo [player]
      alias: ppi

   pploadalts:
      description: reload alt lists from file
      usage: /pploadalts

   ppcheckall:
      description: checkban all accounts
      usage: /ppcheckall

   ppcheck:
      description: checkban the player
      usage: /ppcheck [player]

   ppsetdist:
      description: Sets distance prisoner can move.

   ppsetdamage:
      description: Sets damage prisoner receives.

   pptogglespeech:
      description: Toggles whether prisoner can talk in public chat.

   pptoggledamage:
      description: Toggles whether prisoner can damage players and mobs.

   pptoggleblocks:
      description: Toggles whether prisoner can break blocks.

   ppsetmotd:
      description: Sets prisoner's MOTD.
      

permissions:

   prisonpearl.normal.pplocate:
      description: Gives access to the pplocate command.
      default: true

   prisonpearl.normal.pplocateany:
      description: Gives access to the pplocateany command.
      default: true

   prisonpearl.normal.ppfree:
      description: Gives access to the ppfree command.
      default: true

   prisonpearl.ppfreeany:
      description: Gives access to the ppany command.
      default: false

   prisonpearl.normal.ppsummon:
      description: Gives access to the ppsummon command.
      default: true

   prisonpearl.normal.ppreturn:
      description: Gives access to the ppreturn command.
      default: true

   prisonpearl.normal.ppkill:
      description: Gives access to the ppkill command.
      default: true

   prisonpearl.ppsave:
      description: Gives access to the ppsave command.
      default: false

   prisonpearl.ppimprisonany:
      description: Gives access to the ppimprisonany command.
      default: false

   prisonpearl.ppbroadcast:
      description: Gives access to the ppbroadcast command.
      default: true

   prisonpearl.ppconfirm:
      description: Gives access to the ppconfirm command.
      default: true

   prisonpearl.normal.ppsilence:
      description: Gives access to the ppsilence command.
      default: true

   prisonpearl.pploadalts:
      description: Gives access to the pploadalts command.
      default: false

   prisonpearl.ppcheckall:
      description: Gives access to the ppcheckall command.
      default: false

   prisonpearl.ppcheck:
      description: Gives access to the ppcheck command.
      default: false

   prisonpearl.kill:
      description: Gives access to the kill command.
      default: false

   prisonpearl.normal.ppsetdist:
      description: Gives access to the ppsetdist command.
      default: true

   prisonpearl.normal.ppsetdamage:
      description: Gives access to the ppsetdamage command.
      default: true

   prisonpearl.normal.pptogglespeech:
      description: Gives access to the pptogglespeech command.
      default: true

   prisonpearl.normal.pptoggledamage:
      description: Gives access to the pptoggledamage command.
      default: true

   prisonpearl.normal.pptoggleblocks:
      description: Gives access to the pptoggleblocks command.
      default: true

   prisonpearl.normal.ppsetmotd:
      description: Gives access to the ppsetmotd command.
      default: true

   prisonpearl.*:
      description: Gives full access to PrisonPearl commands
      default: op
      children:
         prisonpearl.locateany: true
         prisonpearl.freeany: true
         prisonpearl.imprisonany: true
         prisonpearl.save: true

   prisonpearl.locateany:
      description: Allows user to use pplocateany to locate prison pearls other than his own
      
   prisonpearl.freeany:
      description: Allows user to use ppfreeany to free prison pearls he does not possess
      
   prisonpearl.imprisonany:
      description: Allows user to use ppimprisonany to imprison any player at will
      
   prisonpearl.save:
      description: Allows user to use ppsave command
```
      
