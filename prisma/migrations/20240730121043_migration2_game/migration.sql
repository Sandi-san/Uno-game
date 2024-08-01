/*
  Warnings:

  - Added the required column `gameId` to the `Deck` table without a default value. This is not possible if the table is not empty.
  - Added the required column `gameId` to the `PlayerData` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Deck" ADD COLUMN     "gameId" INTEGER NOT NULL;

-- AlterTable
ALTER TABLE "PlayerData" ADD COLUMN     "gameId" INTEGER NOT NULL;

-- CreateTable
CREATE TABLE "Game" (
    "id" SERIAL NOT NULL,

    CONSTRAINT "Game_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "PlayerData" ADD CONSTRAINT "PlayerData_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Deck" ADD CONSTRAINT "Deck_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
