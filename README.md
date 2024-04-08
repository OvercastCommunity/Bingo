# PGM Bingo
A hand-picked selection of 25 hidden goals that can be completed on the server.

Everyone gets the same `/bingo` card with the same objectives and order (when someone completes an objective you can click on it in chat to see it on your card). Every completed goal, line, and full house will reward you with raindrops.

Info on the goals can be hidden and revealed throughout the event. When a player discovers a goal they will be rewarded and their completion position stored, solo first place discoveries are also credited.

Originally developed for [OCC's 2024 April Fools](https://discord.com/channels/86514356862320640/220549624530862080/1224342652364001380) event, inspired by [Hypixel Bingo](https://wiki.hypixel.net/Bingo).

## Contribute

We welcome community contributions for new objectives or general plugin improvements.

We have a Google Forms [Bingo Feedback Survey](https://forms.gle/TrcSDVYzKk6tRPxG9) if you have any feedback or ideas following a previous event. 

If you do wish to assist with new objectives please reach out in the [Discord](https://oc.tc/discord). We can make sure your goal ideas doesn't clash with in-development or upcoming goals and ensure they stay secret until the next event. If you want to help but don't have any objectives in mind get in touch too as we have a backlog of over 100 to pick from so no stress.

### Local Setup

You can either use a SQL database (which requires [Database](https://github.com/OvercastCommunity/Database)) or for testing you can review the `config.yml` file which contains an example of how a `mock` database can be setup for testing objectives

Outside of a development setup [Dispense](https://github.com/applenick/Dispense) is required to reward raindrops.

## Current DB "Schema"

### Objectives (bingo_objectives)
- PK slug
- name string
- description (semi-colon for separate lines)
- idx index of item on card
- hint_level how many descriptors to show (1 = only name)
- next_clue_unlock nullable
- discovery_uuid nullable
- discovery_time nullable

### Bingo Progress (bingo_progress)
- player_uuid    COMP PK
- objective_slug COMP PK
- completed boolean default 0
- completed_at timestamp
- placed_position int nullable
- data string column for misc persistent storage

## Build Commands

Run the below formatting command to ensure you follow style conventions.

```
mvn com.coveo:fmt-maven-plugin:format
mvn clean install

# or a combo of the two
mvn com.coveo:fmt-maven-plugin:format clean install
```
