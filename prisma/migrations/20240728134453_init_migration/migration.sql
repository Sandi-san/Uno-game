-- CreateTable
CREATE TABLE "PlayerData" (
    "id" SERIAL NOT NULL,
    "name" TEXT NOT NULL,
    "score" INTEGER NOT NULL,
    "handId" INTEGER NOT NULL,

    CONSTRAINT "PlayerData_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Hand" (
    "id" SERIAL NOT NULL,
    "indexFirst" INTEGER NOT NULL,
    "indexLast" INTEGER NOT NULL,
    "positionArrowRegionLeft" JSONB NOT NULL,
    "boundsArrowRegionLeft" JSONB NOT NULL,
    "positionArrowRegionRight" JSONB NOT NULL,
    "boundsArrowRegionRight" JSONB NOT NULL,

    CONSTRAINT "Hand_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Card" (
    "id" SERIAL NOT NULL,
    "priority" INTEGER NOT NULL,
    "value" INTEGER NOT NULL,
    "color" TEXT NOT NULL,
    "texture" TEXT NOT NULL,
    "position" JSONB NOT NULL,
    "bounds" JSONB NOT NULL,
    "isHighlighted" BOOLEAN NOT NULL,
    "handId" INTEGER NOT NULL,
    "deckId" INTEGER,

    CONSTRAINT "Card_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Deck" (
    "id" SERIAL NOT NULL,
    "size" INTEGER NOT NULL,
    "position" JSONB NOT NULL,
    "bounds" JSONB NOT NULL,

    CONSTRAINT "Deck_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "PlayerData" ADD CONSTRAINT "PlayerData_handId_fkey" FOREIGN KEY ("handId") REFERENCES "Hand"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Card" ADD CONSTRAINT "Card_handId_fkey" FOREIGN KEY ("handId") REFERENCES "Hand"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Card" ADD CONSTRAINT "Card_deckId_fkey" FOREIGN KEY ("deckId") REFERENCES "Deck"("id") ON DELETE SET NULL ON UPDATE CASCADE;
