import { Module } from '@nestjs/common';
import { HandService } from './hand.service';
import { HandController } from './hand.controller';
import { PlayerModule } from 'src/player/player.module';

@Module({
  imports: [PlayerModule],
  providers: [HandService],
  controllers: [HandController],
  exports: [HandService],
})
export class HandModule {}
