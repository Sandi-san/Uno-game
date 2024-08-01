import { CardDto } from 'src/card/dto/create-card.dto';

export class HandDto {
  indexFirst: number;
  indexLast: number;
  positionArrowRegionLeft: object;
  boundsArrowRegionLeft: object;
  positionArrowRegionRight: object;
  boundsArrowRegionRight: object;
  cards: CardDto[];
}