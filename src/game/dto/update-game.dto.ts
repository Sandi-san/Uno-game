// create-game.dto.ts
import { IsArray, IsNumber, IsObject, IsOptional, IsString } from 'class-validator';
import { UpdateDeckDto } from 'src/deck/dto/update-deck.dto';
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