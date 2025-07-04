import { IsArray, IsNumber, IsOptional } from 'class-validator';
import { UpdateCardDto } from 'src/card/dto/update-card.dto';

export class UpdateHandDto {
  @IsNumber()
  @IsOptional()
  id: number;
  @IsNumber()
  @IsOptional()
  indexFirst: number;
  @IsNumber()
  @IsOptional()
  indexLast: number;
  @IsArray()
  @IsOptional()
  cards: UpdateCardDto[];
}