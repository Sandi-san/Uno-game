import { IsNumber, IsOptional, IsString } from "class-validator";

export class CreateCardDto {
    @IsNumber()
    priority: number;
    
    @IsNumber()
    value: number;
    
    @IsString()
    color: string;
        
    @IsString()
    texture: string;
    
    @IsNumber()
    @IsOptional()
    handId?: number;
}