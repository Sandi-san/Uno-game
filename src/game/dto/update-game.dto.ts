// create-game.dto.ts

import { Card } from '@prisma/client';
import { IsArray, IsNumber, IsObject, IsOptional, IsString } from 'class-validator';
import { CreateDeckDto } from 'src/deck/dto/create-deck.dto';
import { UpdateDeckDto } from 'src/deck/dto/update-deck.dto';
import { CreatePlayerDto } from 'src/player/dto/create-player.dto';
import { UpdatePlayerDto } from 'src/player/dto/update-player.dto';

export class UpdateGameDto {
  @IsArray()
  @IsOptional()
  decks: UpdateDeckDto[];

  @IsArray()
  @IsOptional()
  players: UpdatePlayerDto[];

  @IsNumber()
  @IsOptional()
  maxPlayers?: number;

  @IsObject()
  @IsOptional()
  topCard?: Card;

  @IsString()
  @IsOptional()
  gameState: string;

  @IsNumber()
  @IsOptional()
  currentTurn: number;
  
  @IsString()
  @IsOptional()
  turnOrder: string;
}