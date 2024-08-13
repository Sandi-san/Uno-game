-- AlterTable
ALTER TABLE "Game" ADD COLUMN     "currentTurn" INTEGER NOT NULL DEFAULT 1,
ADD COLUMN     "gameState" TEXT NOT NULL DEFAULT 'Paused',
ADD COLUMN     "turnOrder" TEXT NOT NULL DEFAULT 'Clockwise';
