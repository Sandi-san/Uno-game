import { Prisma, Game } from '@prisma/client';
import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateGameDto } from './dto/create-game.dto';
import { DeckService } from 'src/deck/deck.service';
import { PlayerService } from 'src/player/player.service';
import { ObjectId } from 'mongodb';

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
  ) { }

  async create(data: CreateGameDto): Promise<Game> {
    /*
    glupi transactioni

    const game = await this.prisma.game.create({
      data: {
        id: new ObjectId().toHexString(),
      },
    });

    await this.deckService.createMany(game.id, data.decks);
    await this.playerService.createMany(game.id, data.players);
    */
    const { decks, players } = data
     // Create the game first
     const game = await this.prisma.game.create({
      data: {},
    });

    await this.deckService.createMany(game.id, data.decks);
    await this.playerService.createMany(game.id, data.players);
    return game
  }


  async get(id: string): Promise<Game | null> {
    return this.prisma.game.findUnique({
      where: { id },
      include: {
        decks: true,
        players: {
          include: {
            hand: {
              include: {
                cards: true,
              },
            },
          },
        },
      },
    })
  }

  async update(id: string, data: Prisma.GameUpdateInput): Promise<Game> {
    return this.prisma.game.update({
      where: { id },
      data,
      include: {
        decks: true,
        players: {
          include: {
            hand: {
              include: {
                cards: true,
              },
            },
          },
        },
      },
    })
  }

  async delete(id: string): Promise<Game> {
    return this.prisma.game.delete({
      where: { id },
    })
  }
}
