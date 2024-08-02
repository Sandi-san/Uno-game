/*
  Warnings:

  - You are about to drop the column `deckId` on the `Card` table. All the data in the column will be lost.
  - You are about to drop the column `bounds` on the `Deck` table. All the data in the column will be lost.
  - You are about to drop the column `position` on the `Deck` table. All the data in the column will be lost.
  - You are about to drop the `PlayerData` table. If the table is not empty, all the data it contains will be lost.
  - A unique constraint covering the columns `[playerId]` on the table `Hand` will be added. If there are existing duplicate values, this will fail.
  - Added the required column `playerId` to the `Hand` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE "Card" DROP CONSTRAINT "Card_deckId_fkey";

-- DropForeignKey
ALTER TABLE "PlayerData" DROP CONSTRAINT "PlayerData_gameId_fkey";

-- DropForeignKey
ALTER TABLE "PlayerData" DROP CONSTRAINT "PlayerData_handId_fkey";

-- AlterTable
ALTER TABLE "Card" DROP COLUMN "deckId";

-- AlterTable
ALTER TABLE "Deck" DROP COLUMN "bounds",
DROP COLUMN "position";

-- AlterTable
ALTER TABLE "Hand" ADD COLUMN     "playerId" INTEGER NOT NULL;

-- DropTable
DROP TABLE "PlayerData";

-- CreateTable
CREATE TABLE "Player" (
    "id" SERIAL NOT NULL,
    "gameId" INTEGER NOT NULL,
    "name" TEXT NOT NULL,
    "score" INTEGER NOT NULL,

    CONSTRAINT "Player_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "_CardToDeck" (
    "A" INTEGER NOT NULL,
    "B" INTEGER NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "_CardToDeck_AB_unique" ON "_CardToDeck"("A", "B");

-- CreateIndex
CREATE INDEX "_CardToDeck_B_index" ON "_CardToDeck"("B");

-- CreateIndex
CREATE UNIQUE INDEX "Hand_playerId_key" ON "Hand"("playerId");

-- AddForeignKey
ALTER TABLE "Player" ADD CONSTRAINT "Player_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Hand" ADD CONSTRAINT "Hand_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "_CardToDeck" ADD CONSTRAINT "_CardToDeck_A_fkey" FOREIGN KEY ("A") REFERENCES "Card"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "_CardToDeck" ADD CONSTRAINT "_CardToDeck_B_fkey" FOREIGN KEY ("B") REFERENCES "Deck"("id") ON DELETE CASCADE ON UPDATE CASCADE;
