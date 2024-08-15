import { IsArray, IsNumber, IsOptional } from 'class-validator';
import { UpdateCardDto } from 'src/card/dto/update-card.dto';

export class UpdateDeckDto {
  @IsNumber()
  id: number;

  @IsNumber()
  @IsOptional()
  size: number;

  @IsArray()
  @IsOptional()
  cards: UpdateCardDto[];
  
  @IsNumber()
  gameId: number;
}