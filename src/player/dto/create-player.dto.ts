import { IsNumber, IsObject, IsOptional, IsString } from 'class-validator';
import { CreateHandDto } from 'src/hand/dto/create-hand.dto'

export class CreatePlayerDto {
  @IsNumber()
  @IsOptional()
  id?: number;

  @IsString()
  name: string;
  
  @IsNumber()
  score: number;
  
  @IsObject()
  @IsOptional()
  hand?: CreateHandDto;
  
  @IsNumber()
  @IsOptional()
  gameId?: number;
}