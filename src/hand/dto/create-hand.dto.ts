import { IsArray, IsNumber } from 'class-validator';
import { CreateCardDto } from 'src/card/dto/create-card.dto';

export class CreateHandDto {
  @IsNumber()
  indexFirst: number;
  @IsNumber()
  indexLast: number;
  @IsArray()
  cards: CreateCardDto[];
}