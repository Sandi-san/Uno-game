import { IsNumber, IsObject, IsOptional, IsString } from 'class-validator';
import { UpdateHandDto } from 'src/hand/dto/update-hand.dto';

export class UpdatePlayerTurnDto {
  @IsNumber()
  @IsOptional()
  id?: number;

  @IsString()
  @IsOptional()
  name: string;
  
  @IsNumber()
  @IsOptional()
  score: number;
  
  @IsObject()
  @IsOptional()
  hand?: UpdateHandDto;
  
  @IsNumber()
  @IsOptional()
  gameId?: number;

  @IsNumber()
  currentTurn: number;
}