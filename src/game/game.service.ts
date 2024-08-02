import { Prisma, Game } from '@prisma/client';
import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateGameDto } from './dto/create-game.dto';
import { DeckService } from 'src/deck/deck.service';
import { PlayerService } from 'src/player/player.service';
import { DeckDto } from 'src/deck/dto/create-deck.dto';
import { CardDto } from 'src/card/dto/create-card.dto';
import { PlayerDto } from 'src/player/dto/create-player.dto';
import { HandDto } from 'src/hand/dto/create-hand.dto';

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
  ) { }

  async create(data: CreateGameDto): Promise<Game> {
    return await this.prisma.game.create({
      data: {
        decks: {
          create: data.decks.map((deck: DeckDto) => ({
            size: deck.size,
            cards: {
              create: deck.cards
              .filter((card: CardDto | null) => card !== null) // Filter out null values
              .map((card: CardDto) => ({
                priority: card.priority,
                value: card.value,
                color: card.color,
                texture: card.texture,
              }))
            },
          }))
        },
        players: {
          create: data.players.map((player: PlayerDto) => ({
            name: player.name,
            score: player.score,
            hand: player.hand ? {
              create: {
                indexFirst: player.hand.indexFirst,
                indexLast: player.hand.indexLast,
                cards: {
                  create: player.hand.cards
                  .filter((card: CardDto | null) => card !== null) // Filter out null values
                  .map((card: CardDto) => ({
                    priority: card.priority,
                    value: card.value,
                    color: card.color,
                    texture: card.texture,
                  }))
                },  
              },
            } : undefined,
          }))
        },
      }
    });
  }


  async get(id: number): Promise<Game | null> {
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

  async update(id: number, data: Prisma.GameUpdateInput): Promise<Game> {
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

  async delete(id: number): Promise<Game> {
    return this.prisma.game.delete({
      where: { id },
    })
  }
}
