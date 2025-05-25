import { Module } from '@nestjs/common';
import { DeckService } from './deck.service';
import { DeckController } from './deck.controller';
import { CardModule } from 'src/card/card.module';

@Module({
  imports: [CardModule],
  providers: [DeckService],
  controllers: [DeckController],
  exports: [DeckService],

})
export class DeckModule { }
