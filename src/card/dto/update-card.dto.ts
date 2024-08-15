import { IsNumber, IsOptional, IsString } from "class-validator";

export class UpdateCardDto {
    @IsNumber()
    @IsOptional()
    id: number;

    @IsNumber()
    @IsOptional()
    priority: number;
    
    @IsNumber()
    @IsOptional()
    value: number;
    
    @IsString()
    @IsOptional()
    color: string;
        
    @IsString()
    @IsOptional()
    texture: string;
    
    @IsNumber()
    @IsOptional()
    handId?: number;
}