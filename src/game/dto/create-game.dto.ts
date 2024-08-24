// create-game.dto.ts

import { Card } from '@prisma/client';
import { IsArray, IsNumber, IsObject, IsOptional, IsString } from 'class-validator';
import { CreateDeckDto } from 'src/deck/dto/create-deck.dto';
import { CreatePlayerDto } from 'src/player/dto/create-player.dto';

export class CreateGameDto {
  /*
  decks: DeckDto[];
  players: PlayerDto[];
  maxPlayers?: number;
  gameState: string;
  currentTurn: number;
  turnOrder: string;
  */

  @IsArray()
  decks: CreateDeckDto[];

  @IsArray()
  players: CreatePlayerDto[];

  @IsNumber()
  @IsOptional()
  maxPlayers?: number;

  @IsString()
  gameState: string;

  @IsNumber()
  currentTurn: number;
  
  @IsString()
  turnOrder: string;
}