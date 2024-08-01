import { Module } from '@nestjs/common';
import { HandService } from './hand.service';
import { HandController } from './hand.controller';

@Module({
  providers: [HandService],
  controllers: [HandController],
  exports: [HandService],
})
export class HandModule {}
